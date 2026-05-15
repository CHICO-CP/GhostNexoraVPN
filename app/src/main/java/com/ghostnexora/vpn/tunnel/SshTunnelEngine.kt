
    package com.ghostnexora.vpn.tunnel

    import com.ghostnexora.vpn.data.model.ProxyConfig
    import com.ghostnexora.vpn.data.model.VpnProfile
    import com.jcraft.jsch.JSch
    import com.jcraft.jsch.Proxy
    import com.jcraft.jsch.ProxyHTTP
    import com.jcraft.jsch.ProxySOCKS5
    import com.jcraft.jsch.Session
    import com.jcraft.jsch.SocketFactory
    import java.io.IOException
    import java.io.InputStream
    import java.io.OutputStream
    import java.net.InetSocketAddress
    import java.net.Socket
    import java.security.SecureRandom
    import java.security.cert.X509Certificate
    import javax.net.SocketFactory as JvmSocketFactory
    import javax.net.ssl.HostnameVerifier
    import javax.net.ssl.HttpsURLConnection
    import javax.net.ssl.SNIHostName
    import javax.net.ssl.SSLContext
    import javax.net.ssl.SSLParameters
    import javax.net.ssl.SSLSocket
    import javax.net.ssl.TrustManager
    import javax.net.ssl.X509TrustManager

    /**
     * Crea una sesión SSH real usando el modo seleccionado por el perfil.
     *
     * Importante: este cliente cubre la capa de control SSH/TLS/SNI/proxy.
     * El puente de datos VPN/TUN sigue siendo un paso separado.
     */
    class SshTunnelEngine {

        fun connect(profile: VpnProfile): Session {
            val mode = profile.selectedMode
            require(mode.family == "ssh" || mode.family == "ssl") {
                "El motor actual solo soporta perfiles SSH o SSL/SNI. Modo recibido: ${mode.label}"
            }

            val jsch = JSch()
            val session = jsch.getSession(profile.username, profile.host, profile.port)
            session.setPassword(profile.password)
            session.setTimeout(20_000)
            session.setServerAliveInterval(15_000)
            session.setServerAliveCountMax(3)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive,publickey")

            val needsTls = mode.usesTls
            val needsProxy = mode.requiresProxy && profile.proxy.host.isNotBlank() && profile.proxy.port > 0

            if (needsTls || needsProxy) {
                session.setSocketFactory(
                    createSocketFactory(
                        transportHost = profile.host,
                        transportPort = profile.port,
                        sniHost = profile.sni.ifBlank { profile.host },
                        proxy = if (needsProxy) profile.proxy else null,
                        useTls = needsTls
                    )
                )
            }

            session.connect(20_000)
            return session
        }

        fun disconnect(session: Session?) {
            runCatching { session?.disconnect() }
        }

        private fun createSocketFactory(
            transportHost: String,
            transportPort: Int,
            sniHost: String,
            proxy: ProxyConfig?,
            useTls: Boolean
        ): SocketFactory = object : SocketFactory {
            override fun createSocket(host: String, port: Int): Socket {
                val targetHost = host.ifBlank { transportHost }
                val targetPort = if (port > 0) port else transportPort
                val baseSocket = if (proxy == null || proxy.host.isBlank() || proxy.port <= 0) {
                    connectDirect(targetHost, targetPort)
                } else {
                    connectViaProxy(targetHost, targetPort, proxy)
                }

                return if (useTls) {
                    wrapWithTls(baseSocket, sniHost, targetHost, targetPort)
                } else {
                    baseSocket
                }
            }

            override fun getInputStream(socket: Socket): InputStream = socket.getInputStream()
            override fun getOutputStream(socket: Socket): OutputStream = socket.getOutputStream()
        }

        private fun connectDirect(host: String, port: Int): Socket {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.connect(InetSocketAddress(host, port), 15_000)
            return socket
        }

        private fun connectViaProxy(host: String, port: Int, proxy: ProxyConfig): Socket {
            val socket = connectDirect(proxy.host, proxy.port)
            val proxyType = proxy.type.trim().lowercase()

            when (proxyType) {
                "socks5" -> performSocks5Handshake(socket, host, port, proxy)
                else -> performHttpConnectHandshake(socket, host, port, proxy)
            }

            return socket
        }

        private fun performHttpConnectHandshake(socket: Socket, host: String, port: Int, proxy: ProxyConfig) {
            val request = buildString {
                append("CONNECT ")
                append(host)
                append(":")
                append(port)
                append(" HTTP/1.1\r\n")
                append("Host: ")
                append(host)
                append(":")
                append(port)
                append("\r\n")
                append("Proxy-Connection: Keep-Alive\r\n")
                append("\r\n")
            }

            socket.getOutputStream().write(request.toByteArray())
            socket.getOutputStream().flush()

            val response = socket.getInputStream().bufferedReader().readLine().orEmpty()
            if (!response.contains("200")) {
                throw IllegalStateException("HTTP proxy rechazó la conexión: $response")
            }
        }

        private fun performSocks5Handshake(socket: Socket, host: String, port: Int, proxy: ProxyConfig) {
            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            out.write(byteArrayOf(0x05, 0x01, 0x00))
            out.flush()
            val methodResponse = ByteArray(2)
            readFully(input, methodResponse)
            if (methodResponse[1].toInt() != 0x00) {
                throw IllegalStateException("SOCKS5 requirió autenticación no soportada")
            }

            val hostBytes = host.toByteArray()
            val request = ByteArray(7 + hostBytes.size)
            request[0] = 0x05
            request[1] = 0x01
            request[2] = 0x00
            request[3] = 0x03
            request[4] = hostBytes.size.toByte()
            hostBytes.copyInto(request, destinationOffset = 5)
            val portIndex = 5 + hostBytes.size
            request[portIndex] = ((port shr 8) and 0xFF).toByte()
            request[portIndex + 1] = (port and 0xFF).toByte()
            out.write(request)
            out.flush()

            val response = ByteArray(10)
            readFully(input, response)
            if (response[1].toInt() != 0x00) {
                throw IllegalStateException("SOCKS5 rechazó la conexión")
            }
        }

        private fun readFully(input: InputStream, buffer: ByteArray) {
            var offset = 0
            while (offset < buffer.size) {
                val read = input.read(buffer, offset, buffer.size - offset)
                if (read < 0) throw IOException("Socket cerrado durante handshake")
                offset += read
            }
        }

        private fun wrapWithTls(socket: Socket, sniHost: String, peerHost: String, peerPort: Int): SSLSocket {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(TrustAllX509TrustManager()), SecureRandom())

            val sslSocket = sslContext.socketFactory.createSocket(socket, peerHost, peerPort, true) as SSLSocket
            sslSocket.useClientMode = true

            val params: SSLParameters = sslSocket.sslParameters
            params.serverNames = listOf(SNIHostName(sniHost))
            sslSocket.sslParameters = params
            sslSocket.startHandshake()
            return sslSocket
        }
    }

    private class TrustAllX509TrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
