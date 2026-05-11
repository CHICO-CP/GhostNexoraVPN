package com.ghostnexora.vpn.data.model

/**
 * Representa todos los estados posibles de la conexión VPN.
 * Usado por el servicio, ViewModel y UI para reaccionar al estado actual.
 */
sealed class VpnConnectionState {

    /** Sin conexión activa */
    object Disconnected : VpnConnectionState()

    /** Intentando establecer conexión */
    data class Connecting(val profileName: String = "") : VpnConnectionState()

    /** Conexión activa y estable */
    data class Connected(
        val profileName: String = "",
        val serverIp: String = "",
        val connectedSince: Long = System.currentTimeMillis()
    ) : VpnConnectionState()

    /** Cancelando la conexión en curso */
    object Disconnecting : VpnConnectionState()

    /** Error al conectar o conexión perdida */
    data class Error(
        val message: String = "",
        val profileName: String = ""
    ) : VpnConnectionState()

    // ── Helpers ──────────────────────────────────────────

    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting
    val isDisconnected: Boolean get() = this is Disconnected
    val hasError: Boolean get() = this is Error

    /** Etiqueta corta para mostrar en UI */
    fun label(): String = when (this) {
        is Disconnected  -> "Desconectado"
        is Connecting    -> "Conectando…"
        is Connected     -> "Conectado"
        is Disconnecting -> "Desconectando…"
        is Error         -> "Error"
    }

    /** Texto del botón de acción principal en Dashboard */
    fun actionLabel(): String = when (this) {
        is Disconnected  -> "Conectar"
        is Connecting    -> "Cancelar"
        is Connected     -> "Desconectar"
        is Disconnecting -> "Cancelando…"
        is Error         -> "Reintentar"
    }
}
