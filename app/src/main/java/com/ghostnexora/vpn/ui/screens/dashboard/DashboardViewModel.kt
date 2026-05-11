package com.ghostnexora.vpn.ui.screens.dashboard

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostnexora.vpn.data.model.VpnConnectionState
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.data.repository.ProfileRepository
import com.ghostnexora.vpn.service.GhostVpnService
import com.ghostnexora.vpn.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del Dashboard.
 *
 * Responsabilidades:
 * - Observar el perfil activo desde DataStore + Room
 * - Gestionar el estado de conexión VPN
 * - Mantener el timer de sesión actualizado cada segundo
 * - Solicitar/validar el permiso VPN antes de conectar
 * - Exponer UiState reactivo a la pantalla
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ProfileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ══════════════════════════════════════════════════════════════════════
    // UI STATE
    // ══════════════════════════════════════════════════════════════════════

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // ══════════════════════════════════════════════════════════════════════
    // ESTADO INTERNO
    // ══════════════════════════════════════════════════════════════════════

    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0L

    // ══════════════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════════════

    init {
        observeActiveProfile()
        observeAllProfiles()
    }

    // ══════════════════════════════════════════════════════════════════════
    // OBSERVADORES
    // ══════════════════════════════════════════════════════════════════════

    private fun observeActiveProfile() {
        viewModelScope.launch {
            repository.activeProfileId
                .flatMapLatest { id ->
                    if (id.isEmpty()) flowOf(null)
                    else repository.observeProfile(id)
                }
                .collectLatest { profile ->
                    _uiState.update { it.copy(activeProfile = profile) }
                }
        }
    }

    private fun observeAllProfiles() {
        viewModelScope.launch {
            repository.profileCount
                .collectLatest { count ->
                    _uiState.update { it.copy(hasProfiles = count > 0) }
                }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACCIONES PRINCIPALES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Acción principal del botón del Dashboard.
     * Decide qué hacer según el estado actual.
     */
    fun onMainAction(activity: Activity) {
        when (_uiState.value.connectionState) {
            is VpnConnectionState.Disconnected,
            is VpnConnectionState.Error -> requestConnect(activity)

            is VpnConnectionState.Connecting    -> cancelConnect()
            is VpnConnectionState.Connected     -> disconnect()
            is VpnConnectionState.Disconnecting -> { /* esperar */ }
        }
    }

    /**
     * Solicita conexión VPN.
     * Primero verifica si hay permiso; si no, solicita al usuario.
     */
    private fun requestConnect(activity: Activity) {
        val profile = _uiState.value.activeProfile
        if (profile == null) {
            _uiState.update { it.copy(snackbarMessage = "Selecciona un perfil primero") }
            return
        }

        // Verificar permiso VPN
        val permissionIntent = VpnService.prepare(activity)
        if (permissionIntent != null) {
            // Necesita permiso → emitir evento para que la UI lance el diálogo
            _uiState.update {
                it.copy(pendingVpnPermissionIntent = permissionIntent)
            }
            return
        }

        // Permiso ya concedido → conectar
        connect(profile)
    }

    /**
     * Llamado por la UI cuando el usuario aprueba el permiso VPN.
     */
    fun onVpnPermissionGranted() {
        val profile = _uiState.value.activeProfile ?: return
        _uiState.update { it.copy(pendingVpnPermissionIntent = null) }
        connect(profile)
    }

    /**
     * Llamado por la UI cuando el usuario rechaza el permiso VPN.
     */
    fun onVpnPermissionDenied() {
        _uiState.update {
            it.copy(
                pendingVpnPermissionIntent = null,
                snackbarMessage = "Permiso VPN requerido para conectar"
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONEXIÓN / DESCONEXIÓN
    // ══════════════════════════════════════════════════════════════════════

    private fun connect(profile: VpnProfile) {
        viewModelScope.launch {
            updateState(VpnConnectionState.Connecting(profile.name))

            // Lanzar el servicio VPN
            val intent = Intent(context, GhostVpnService::class.java).apply {
                action = GhostVpnService.ACTION_CONNECT
                putExtra(GhostVpnService.EXTRA_PROFILE_ID, profile.id)
            }
            context.startForegroundService(intent)

            // Marcar último uso en DB
            repository.markLastUsed(profile.id)

            // Simular handshake (el servicio real emitirá el estado)
            // En producción esto viene del servicio via broadcast/StateFlow compartido
            delay(1500)
            onConnected(profile)
        }
    }

    private fun onConnected(profile: VpnProfile) {
        sessionStartTime = System.currentTimeMillis()
        updateState(
            VpnConnectionState.Connected(
                profileName  = profile.name,
                serverIp     = profile.host,
                connectedSince = sessionStartTime
            )
        )
        startSessionTimer()
    }

    fun disconnect() {
        stopSessionTimer()
        updateState(VpnConnectionState.Disconnecting)

        val intent = Intent(context, GhostVpnService::class.java).apply {
            action = GhostVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)

        viewModelScope.launch {
            delay(600)
            updateState(VpnConnectionState.Disconnected)
            _uiState.update { it.copy(sessionElapsed = 0L) }
        }
    }

    private fun cancelConnect() {
        stopSessionTimer()
        val intent = Intent(context, GhostVpnService::class.java).apply {
            action = GhostVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
        updateState(VpnConnectionState.Disconnected)
    }

    // ══════════════════════════════════════════════════════════════════════
    // TIMER DE SESIÓN
    // ══════════════════════════════════════════════════════════════════════

    private fun startSessionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - sessionStartTime
                _uiState.update { it.copy(sessionElapsed = elapsed) }
                delay(1000L)
            }
        }
    }

    private fun stopSessionTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ══════════════════════════════════════════════════════════════════════
    // SELECCIÓN DE PERFIL
    // ══════════════════════════════════════════════════════════════════════

    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            repository.setActiveProfileId(profileId)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private fun updateState(state: VpnConnectionState) {
        _uiState.update { it.copy(connectionState = state) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopSessionTimer()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// UI STATE
// ══════════════════════════════════════════════════════════════════════════

data class DashboardUiState(
    val connectionState: VpnConnectionState  = VpnConnectionState.Disconnected,
    val activeProfile: VpnProfile?           = null,
    val hasProfiles: Boolean                 = false,
    val sessionElapsed: Long                 = 0L,
    val snackbarMessage: String?             = null,
    val pendingVpnPermissionIntent: Intent?  = null
) {
    val isConnected: Boolean    get() = connectionState is VpnConnectionState.Connected
    val isConnecting: Boolean   get() = connectionState is VpnConnectionState.Connecting
    val isDisconnected: Boolean get() = connectionState is VpnConnectionState.Disconnected
    val hasError: Boolean       get() = connectionState is VpnConnectionState.Error

    val serverIp: String
        get() = (connectionState as? VpnConnectionState.Connected)?.serverIp ?: "--"
}
