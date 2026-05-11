package com.ghostnexora.vpn.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.ghostnexora.vpn.GhostNexoraApp
import com.ghostnexora.vpn.R
import com.ghostnexora.vpn.data.model.LogLevel
import com.ghostnexora.vpn.data.model.VpnConnectionState
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.data.repository.ProfileRepository
import com.ghostnexora.vpn.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import javax.inject.Inject

@AndroidEntryPoint
class GhostVpnService : VpnService() {

    @Inject
    lateinit var repository: ProfileRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var tunInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private var udpChannel: DatagramChannel? = null
    private var activeProfile: VpnProfile? = null

    private val binder = GhostVpnBinder()

    // ══════════════════════════════════════════════════════════════════════
    // COMPANION OBJECT — único, con todas las constantes
    // ══════════════════════════════════════════════════════════════════════

    companion object {
        const val ACTION_CONNECT     = "com.ghostnexora.vpn.CONNECT"
        const val ACTION_DISCONNECT  = "com.ghostnexora.vpn.DISCONNECT"
        const val EXTRA_PROFILE_ID   = "extra_profile_id"
        const val MAX_PACKET_SIZE    = 32767

        private val _connectionState = MutableStateFlow<VpnConnectionState>(
            VpnConnectionState.Disconnected
        )
        val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

        fun updateState(state: VpnConnectionState) {
            _connectionState.value = state
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CICLO DE VIDA
    // ══════════════════════════════════════════════════════════════════════

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) {
                    serviceScope.launch { handleConnect(profileId) }
                } else {
                    serviceScope.launch {
                        logSafe(LogLevel.ERROR, "No se especificó perfil para conectar")
                        updateState(VpnConnectionState.Error("Sin perfil especificado"))
                        stopSelf()
                    }
                }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch { handleDisconnect() }
            }
            else -> {
                serviceScope.launch { handleSystemRestart() }
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        serviceScope.launch {
            logSafe(LogLevel.WARNING, "Permiso VPN revocado por el sistema")
            handleDisconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch { handleDisconnect() }
        serviceScope.cancel()
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONEXIÓN
    // ══════════════════════════════════════════════════════════════════════

    private suspend fun handleConnect(profileId: String) {
        try {
            val profile = repository.getProfileById(profileId)
                ?: run {
                    updateState(VpnConnectionState.Error("Perfil no encontrado"))
                    logSafe(LogLevel.ERROR, "Perfil $profileId no encontrado")
                    return
                }

            activeProfile = profile
            updateState(VpnConnectionState.Connecting(profile.name))
            startForeground(
                GhostNexoraApp.NOTIF_ID_VPN,
                buildNotification(VpnConnectionState.Connecting(profile.name))
            )
            logSafe(LogLevel.INFO, "Iniciando conexión: ${profile.name}", profile.id)

            connectTunnel(profile)

            val tun = buildTunInterface(profile)
                ?: run {
                    updateState(VpnConnectionState.Error("No se pudo crear la interfaz TUN"))
                    logSafe(LogLevel.ERROR, "Fallo al crear TUN", profile.id)
                    return
                }

            tunInterface = tun
            repository.markLastUsed(profile.id)

            val connectedState = VpnConnectionState.Connected(
                profileName    = profile.name,
                serverIp       = profile.host,
                connectedSince = System.currentTimeMillis()
            )
            updateState(connectedState)
            updateNotification(connectedState)
            logSafe(LogLevel.SUCCESS, "VPN conectada: ${profile.host}:${profile.port}", profile.id)

            startPacketForwarding(tun)

        } catch (e: Exception) {
            val msg = e.message ?: "Error desconocido al conectar"
            updateState(VpnConnectionState.Error(msg))
            logSafe(LogLevel.ERROR, "Error de conexión: $msg")
            updateNotification(VpnConnectionState.Error(msg))
        }
    }

    private suspend fun handleDisconnect() {
        try {
            logSafe(LogLevel.INFO, "Desconectando VPN…", activeProfile?.id)
            updateState(VpnConnectionState.Disconnecting)

            tunnelJob?.cancelAndJoin()
            tunnelJob = null

            udpChannel?.close()
            udpChannel = null

            tunInterface?.close()
            tunInterface = null

            activeProfile = null
            updateState(VpnConnectionState.Disconnected)
            logSafe(LogLevel.INFO, "VPN desconectada correctamente")

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

        } catch (e: Exception) {
            logSafe(LogLevel.ERROR, "Error al desconectar: ${e.message}")
            updateState(VpnConnectionState.Disconnected)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun handleSystemRestart() {
        val profileId      = repository.activeProfileId.first()
        val shouldReconnect = repository.autoReconnect.first()
        if (shouldReconnect && profileId.isNotEmpty()) {
            logSafe(LogLevel.INFO, "Reconectando tras reinicio del sistema")
            handleConnect(profileId)
        } else {
            stopSelf()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // INTERFAZ TUN
    // ══════════════════════════════════════════════════════════════════════

    private fun buildTunInterface(profile: VpnProfile): ParcelFileDescriptor? {
        return try {
            Builder()
                .setSession(profile.name)
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1500)
                .setBlocking(true)
                .establish()
        } catch (e: Exception) {
            serviceScope.launch {
                logSafe(LogLevel.ERROR, "Error creando TUN: ${e.message}")
            }
            null
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TÚNEL UDP
    // ══════════════════════════════════════════════════════════════════════

    private fun connectTunnel(profile: VpnProfile) {
        udpChannel = DatagramChannel.open().also { channel ->
            protect(channel.socket())
            channel.configureBlocking(false)
            channel.connect(InetSocketAddress(profile.host, profile.port))
        }
        serviceScope.launch {
            logSafe(LogLevel.DEBUG, "Canal UDP abierto → ${profile.host}:${profile.port}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // BUCLE DE PAQUETES
    // ══════════════════════════════════════════════════════════════════════

    private fun startPacketForwarding(tun: ParcelFileDescriptor) {
        tunnelJob = serviceScope.launch(Dispatchers.IO) {
            val inputStream  = FileInputStream(tun.fileDescriptor)
            val outputStream = FileOutputStream(tun.fileDescriptor)
            val buffer       = ByteBuffer.allocate(MAX_PACKET_SIZE)
            val packet       = ByteArray(MAX_PACKET_SIZE)

            logSafe(LogLevel.DEBUG, "Bucle de reenvío de paquetes iniciado")

            while (isActive) {
                try {
                    val bytesRead = inputStream.read(packet)
                    if (bytesRead > 0) {
                        buffer.clear()
                        buffer.put(packet, 0, bytesRead)
                        buffer.flip()
                        udpChannel?.write(buffer)
                    }

                    buffer.clear()
                    val bytesFromServer = udpChannel?.read(buffer) ?: 0
                    if (bytesFromServer > 0) {
                        outputStream.write(buffer.array(), 0, bytesFromServer)
                    }

                    delay(1L)

                } catch (e: Exception) {
                    if (isActive) {
                        logSafe(LogLevel.WARNING, "Error en bucle de paquetes: ${e.message}")
                        delay(500L)
                    }
                }
            }

            logSafe(LogLevel.DEBUG, "Bucle de paquetes finalizado")
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // NOTIFICACIONES
    // ══════════════════════════════════════════════════════════════════════

    private fun buildNotification(state: VpnConnectionState): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GhostVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, content) = when (state) {
            is VpnConnectionState.Connected     ->
                "VPN Conectada" to "Servidor: ${state.serverIp} · ${state.profileName}"
            is VpnConnectionState.Connecting    ->
                "Conectando VPN…" to "Perfil: ${state.profileName}"
            is VpnConnectionState.Disconnecting ->
                "Desconectando…" to "Cerrando la conexión VPN"
            is VpnConnectionState.Error         ->
                "Error de VPN" to state.message
            else ->
                "Ghost Nexora VPN" to "Desconectado"
        }

        val builder = NotificationCompat.Builder(this, GhostNexoraApp.CHANNEL_VPN_STATUS)
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (state is VpnConnectionState.Connected) {
            builder.addAction(
                R.drawable.ic_vpn_notification,
                "Desconectar",
                disconnectIntent
            )
        }

        return builder.build()
    }

    private fun updateNotification(state: VpnConnectionState) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(GhostNexoraApp.NOTIF_ID_VPN, buildNotification(state))
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Log seguro — puede llamarse tanto desde coroutines como desde
     * funciones normales lanzando una nueva coroutine si hace falta.
     */
    private suspend fun logSafe(
        level: LogLevel,
        message: String,
        profileId: String? = null
    ) = repository.log(level, message, profileId, tag = "GhostVPN")

    inner class GhostVpnBinder : Binder() {
        fun getService(): GhostVpnService = this@GhostVpnService
    }
}
