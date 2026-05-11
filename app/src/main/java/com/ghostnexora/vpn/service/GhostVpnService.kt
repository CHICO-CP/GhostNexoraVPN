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

/**
 * GhostVpnService — Servicio VPN principal de Ghost Nexora.
 *
 * Extiende [VpnService] (Android) para crear una interfaz TUN real.
 * Se ejecuta como Foreground Service con notificación persistente.
 *
 * Ciclo de vida:
 *  1. onStartCommand → ACTION_CONNECT  → connectVpn()
 *  2. onStartCommand → ACTION_DISCONNECT → disconnectVpn()
 *  3. onDestroy → limpieza de recursos
 *
 * Estado global expuesto vía [connectionState] (StateFlow compartido).
 */
@AndroidEntryPoint
class GhostVpnService : VpnService() {

    @Inject
    lateinit var repository: ProfileRepository

    // ── Scope propio del servicio ─────────────────────────────────────────
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Interfaz TUN ──────────────────────────────────────────────────────
    private var tunInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null

    // ── Canal UDP para el túnel ───────────────────────────────────────────
    private var udpChannel: DatagramChannel? = null

    // ── Perfil activo en esta sesión ──────────────────────────────────────
    private var activeProfile: VpnProfile? = null

    // ── Binder para comunicación local (opcional) ─────────────────────────
    private val binder = GhostVpnBinder()

    // ══════════════════════════════════════════════════════════════════════
    // ESTADO GLOBAL (Singleton compartido con ViewModels)
    // ══════════════════════════════════════════════════════════════════════

    companion object {
        const val ACTION_CONNECT    = "com.ghostnexora.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.ghostnexora.vpn.DISCONNECT"
        const val EXTRA_PROFILE_ID  = "extra_profile_id"

        // StateFlow global — compartido entre servicio y ViewModels
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
                    logAndStop("No se especificó perfil para conectar")
                }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch { handleDisconnect() }
            }
            else -> {
                // Reinicio del sistema — intentar reconectar
                serviceScope.launch { handleSystemRestart() }
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        // Android revocó el permiso VPN (usuario desconectó desde ajustes)
        serviceScope.launch {
            log(LogLevel.WARNING, "Permiso VPN revocado por el sistema")
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
            // 1. Cargar perfil
            val profile = repository.getProfileById(profileId)
                ?: run {
                    updateState(VpnConnectionState.Error("Perfil no encontrado"))
                    log(LogLevel.ERROR, "Perfil $profileId no encontrado")
                    return
                }

            activeProfile = profile
            updateState(VpnConnectionState.Connecting(profile.name))
            startForeground(GhostNexoraApp.NOTIF_ID_VPN, buildNotification(VpnConnectionState.Connecting(profile.name)))
            log(LogLevel.INFO, "Iniciando conexión: ${profile.name}", profile.id)

            // 2. Establecer canal de túnel al servidor
            connectTunnel(profile)

            // 3. Configurar interfaz TUN
            val tun = buildTunInterface(profile)
                ?: run {
                    updateState(VpnConnectionState.Error("No se pudo crear la interfaz TUN"))
                    log(LogLevel.ERROR, "Fallo al crear TUN", profile.id)
                    return
                }

            tunInterface = tun

            // 4. Actualizar estado → Conectado
            repository.markLastUsed(profile.id)
            val connectedState = VpnConnectionState.Connected(
                profileName    = profile.name,
                serverIp       = profile.host,
                connectedSince = System.currentTimeMillis()
            )
            updateState(connectedState)
            updateNotification(connectedState)
            log(LogLevel.SUCCESS, "VPN conectada: ${profile.host}:${profile.port}", profile.id)

            // 5. Iniciar bucle de reenvío de paquetes
            startPacketForwarding(tun)

        } catch (e: Exception) {
            val msg = e.message ?: "Error desconocido al conectar"
            updateState(VpnConnectionState.Error(msg))
            log(LogLevel.ERROR, "Error de conexión: $msg")
            updateNotification(VpnConnectionState.Error(msg))
        }
    }

    private suspend fun handleDisconnect() {
        try {
            log(LogLevel.INFO, "Desconectando VPN…", activeProfile?.id)
            updateState(VpnConnectionState.Disconnecting)

            // Cancelar bucle de paquetes
            tunnelJob?.cancelAndJoin()
            tunnelJob = null

            // Cerrar canal UDP
            udpChannel?.close()
            udpChannel = null

            // Cerrar interfaz TUN
            tunInterface?.close()
            tunInterface = null

            activeProfile = null
            updateState(VpnConnectionState.Disconnected)
            log(LogLevel.INFO, "VPN desconectada correctamente")

            // Detener el foreground service
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

        } catch (e: Exception) {
            log(LogLevel.ERROR, "Error al desconectar: ${e.message}")
            updateState(VpnConnectionState.Disconnected)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun handleSystemRestart() {
        // Si el servicio fue reiniciado por Android, intentar reconectar
        val profileId = repository.activeProfileId.first()
        val shouldReconnect = repository.autoReconnect.first()

        if (shouldReconnect && profileId.isNotEmpty()) {
            log(LogLevel.INFO, "Reconectando tras reinicio del sistema")
            handleConnect(profileId)
        } else {
            stopSelf()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // INTERFAZ TUN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Construye la interfaz TUN virtual usando VpnService.Builder.
     *
     * Configura:
     * - IP local del dispositivo dentro del túnel (10.0.0.2)
     * - Ruta por defecto (0.0.0.0/0) → todo el tráfico pasa por VPN
     * - DNS seguro (Cloudflare / Google)
     * - MTU estándar de 1500 bytes
     */
    private fun buildTunInterface(profile: VpnProfile): ParcelFileDescriptor? {
        return try {
            Builder()
                .setSession(profile.name)
                .addAddress("10.0.0.2", 32)            // IP TUN local
                .addRoute("0.0.0.0", 0)                 // Ruta por defecto (todo el tráfico)
                .addRoute("::", 0)                       // IPv6
                .addDnsServer("1.1.1.1")                // Cloudflare primario
                .addDnsServer("1.0.0.1")                // Cloudflare secundario
                .addDnsServer("8.8.8.8")                // Google fallback
                .setMtu(1500)
                .setBlocking(true)
                .establish()
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Error creando TUN: ${e.message}")
            null
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TÚNEL UDP AL SERVIDOR
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Abre el canal UDP hacia el servidor VPN.
     * protect() es esencial para que el tráfico del túnel NO pase
     * por la propia interfaz TUN (evitar bucle infinito).
     */
    private fun connectTunnel(profile: VpnProfile) {
        udpChannel = DatagramChannel.open().also { channel ->
            protect(channel.socket())
            channel.configureBlocking(false)
            channel.connect(
                InetSocketAddress(profile.host, profile.port)
            )
        }
        log(LogLevel.DEBUG, "Canal UDP abierto → ${profile.host}:${profile.port}")
    }

    // ══════════════════════════════════════════════════════════════════════
    // BUCLE DE REENVÍO DE PAQUETES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Bucle principal de la VPN:
     * - Lee paquetes IP de la interfaz TUN (del dispositivo)
     * - Los envía por el canal UDP al servidor
     * - Lee respuestas del servidor y las escribe en TUN
     *
     * Corre en una coroutine hasta que se cancela.
     */
    private fun startPacketForwarding(tun: ParcelFileDescriptor) {
        tunnelJob = serviceScope.launch(Dispatchers.IO) {
            val inputStream  = FileInputStream(tun.fileDescriptor)
            val outputStream = FileOutputStream(tun.fileDescriptor)
            val buffer       = ByteBuffer.allocate(MAX_PACKET_SIZE)
            val packet       = ByteArray(MAX_PACKET_SIZE)

            log(LogLevel.DEBUG, "Bucle de reenvío de paquetes iniciado")

            while (isActive) {
                try {
                    // ── Leer paquete del TUN (tráfico del dispositivo) ─────
                    val bytesRead = inputStream.read(packet)
                    if (bytesRead > 0) {
                        buffer.clear()
                        buffer.put(packet, 0, bytesRead)
                        buffer.flip()
                        udpChannel?.write(buffer)
                    }

                    // ── Leer respuesta del servidor ────────────────────────
                    buffer.clear()
                    val bytesFromServer = udpChannel?.read(buffer) ?: 0
                    if (bytesFromServer > 0) {
                        outputStream.write(buffer.array(), 0, bytesFromServer)
                    }

                    // Pequeña pausa para no saturar CPU
                    delay(1L)

                } catch (e: Exception) {
                    if (isActive) {
                        log(LogLevel.WARNING, "Error en bucle de paquetes: ${e.message}")
                        // En producción: implementar retry con backoff
                        delay(500L)
                    }
                }
            }

            log(LogLevel.DEBUG, "Bucle de paquetes finalizado")
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // NOTIFICACIONES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Construye la notificación persistente del foreground service.
     * Es obligatoria por Android para mantener el servicio vivo.
     */
    private fun buildNotification(state: VpnConnectionState): Notification {
        // Intent para abrir la app al tocar la notificación
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para desconectar desde la notificación
        val disconnectIntent = PendingIntent.getService(
            this,
            1,
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

        // Acción de desconectar solo cuando está conectado
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

    private fun logAndStop(message: String) {
        serviceScope.launch {
            log(LogLevel.ERROR, message)
            updateState(VpnConnectionState.Error(message))
            stopSelf()
        }
    }

    private suspend fun log(
        level: LogLevel,
        message: String,
        profileId: String? = null
    ) = repository.log(level, message, profileId, tag = "GhostVPN")

    // ══════════════════════════════════════════════════════════════════════
    // BINDER
    // ══════════════════════════════════════════════════════════════════════

    inner class GhostVpnBinder : Binder() {
        fun getService(): GhostVpnService = this@GhostVpnService
    }

    companion object {
        private const val MAX_PACKET_SIZE = 32767
    }
}
