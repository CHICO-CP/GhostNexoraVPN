package com.ghostnexora.vpn.tunnel

import com.ghostnexora.vpn.data.model.ProxyConfig
import com.ghostnexora.vpn.data.model.VpnProfile
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Motor de conexión SSH sobre transporte directo, proxy y/o TLS/SNI.
 *
 * Esta implementación usa SSHJ como capa SSH y sockets estándar para el
 * transporte. Cuando el modo requiere TLS/SNI, la conexión SSH se monta
 * sobre un SSLSocket ya negociado.
 */
class SshTunnelEngine {

    fun connect(profile: VpnProfile): SSHClient {
        val mode = profile.selectedMode
        require(mode.family == "ssh" || mode.family == "ssl") {
            "El motor actual solo soporta perfiles SSH o SSL/SNI. Modo recibido: ${mode.label}"
        }

        val transportHost = profile.host.trim()
        val transportPort = profile.port.coerceIn(1, 65535)
        val sniHost = profile.sni.ifBlank { transportHost }
        val proxy = profile.proxy.takeIf { it.host.isNotBlank() && it.port in 1..65535 }

        val ssh = SSHClient()
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        ssh.setConnectTimeout(20_000)
        ssh.setTimeout(20_000)

        when {
            proxy != null || mode.usesTls -> {
                val baseSocket = openTransportSocket(transportHost, transportPort, proxy)
                val finalSocket = if (mode.usesTls) {
                    wrapWithTls(baseSocket, sniHost, transportHost, transportPort)
                } else {
                    baseSocket
                }

                ssh.connectVia(finalSocket.inputStream, finalSocket.outputStream)
            }
            else -> {
                ssh.connect(transportHost, transportPort)
            }
        }

        if (profile.username.isNotBlank()) {
            ssh.authPassword(profile.username, profile.password)
        }

        return ssh
    }

    fun disconnect(client: SSHClient?) {
        runCatching { client?.disconnect() }
        runCatching { client?.close() }
    }

    private fun openTransportSocket(
        transportHost: String,
        transportPort: Int,
        proxy: ProxyConfig?
    ): Socket {
        return if (proxy == null) {
            connectDirect(transportHost, transportPort)
        } else {
            connectThroughProxy(transportHost, transportPort, proxy)
        }
    }

    private fun connectDirect(host: String, port: Int): Socket {
        return Socket().apply {
            tcpNoDelay = true
            keepAlive = true
            connect(InetSocketAddress(host, port), 15_000)
        }
    }

    private fun connectThroughProxy(host: String, port: Int, proxy: ProxyConfig): Socket {
        val socket = connectDirect(proxy.host.trim(), proxy.port)
        when (proxy.type.trim().lowercase()) {
            "socks5" -> performSocks5Handshake(socket, host, port)
            else -> performHttpConnectHandshake(socket, host, port)
        }
        return socket
    }

    private fun performHttpConnectHandshake(socket: Socket, host: String, port: Int) {
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

        val response = BufferedReader(socket.getInputStream().reader()).readLine().orEmpty()
        if (!response.contains("200")) {
            throw IOException("HTTP proxy rechazó la conexión: $response")
        }
    }
    private fun performSocks5Handshake(socket: Socket, host: String, port: Int) {
        val out = socket.getOutputStream()
        val input = socket.getInputStream()

        out.write(byteArrayOf(0x05, 0x01, 0x00))
        out.flush()
        val methodResponse = ByteArray(2)
        readFully(input, methodResponse)
        if (methodResponse[1].toInt() != 0x00) {
            throw IOException("SOCKS5 requirió autenticación no soportada")
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
            throw IOException("SOCKS5 rechazó la conexión")
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
