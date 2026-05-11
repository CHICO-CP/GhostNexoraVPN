package com.ghostnexora.vpn.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostnexora.vpn.data.model.ProxyConfig
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.data.repository.ProfileRepository
import com.ghostnexora.vpn.util.isValidHost
import com.ghostnexora.vpn.util.isValidPort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateEditViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditUiState())
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    // ══════════════════════════════════════════════════════════════════════
    // CARGA DEL PERFIL (modo edición)
    // ══════════════════════════════════════════════════════════════════════

    fun loadProfile(profileId: String?) {
        if (profileId == null) {
            _uiState.update { it.copy(isEditMode = false, isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = repository.getProfileById(profileId)
            if (profile != null) {
                _uiState.update {
                    it.copy(
                        isEditMode   = true,
                        isLoading    = false,
                        profileId    = profile.id,
                        name         = profile.name,
                        host         = profile.host,
                        port         = profile.port.toString(),
                        username     = profile.username,
                        password     = profile.password,
                        method       = profile.method,
                        sslEnabled   = profile.sslEnabled,
                        sni          = profile.sni,
                        proxyHost    = profile.proxy.host,
                        proxyPort    = profile.proxy.port.toString().let {
                            p -> if (p == "0") "" else p
                        },
                        proxyType    = profile.proxy.type,
                        tags         = profile.tagsRaw,
                        notes        = profile.notes,
                        enabled      = profile.enabled
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Perfil no encontrado") }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACTUALIZACIÓN DE CAMPOS
    // ══════════════════════════════════════════════════════════════════════

    fun onNameChange(v: String)       = _uiState.update { it.copy(name = v, nameError = null) }
    fun onHostChange(v: String)       = _uiState.update { it.copy(host = v, hostError = null) }
    fun onPortChange(v: String)       = _uiState.update { it.copy(port = v, portError = null) }
    fun onUsernameChange(v: String)   = _uiState.update { it.copy(username = v) }
    fun onPasswordChange(v: String)   = _uiState.update { it.copy(password = v) }
    fun onMethodChange(v: String)     = _uiState.update { it.copy(method = v) }
    fun onSslChange(v: Boolean)       = _uiState.update { it.copy(sslEnabled = v) }
    fun onSniChange(v: String)        = _uiState.update { it.copy(sni = v) }
    fun onProxyHostChange(v: String)  = _uiState.update { it.copy(proxyHost = v) }
    fun onProxyPortChange(v: String)  = _uiState.update { it.copy(proxyPort = v) }
    fun onProxyTypeChange(v: String)  = _uiState.update { it.copy(proxyType = v) }
    fun onTagsChange(v: String)       = _uiState.update { it.copy(tags = v) }
    fun onNotesChange(v: String)      = _uiState.update { it.copy(notes = v) }
    fun onEnabledChange(v: Boolean)   = _uiState.update { it.copy(enabled = v) }
    fun togglePasswordVisible()       = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleAdvancedSection()       = _uiState.update { it.copy(showAdvanced = !it.showAdvanced) }
    fun clearError()                  = _uiState.update { it.copy(error = null) }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDACIÓN
    // ══════════════════════════════════════════════════════════════════════

    private fun validate(): Boolean {
        val s = _uiState.value
        var valid = true

        if (s.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es obligatorio") }
            valid = false
        }
        if (s.host.isBlank()) {
            _uiState.update { it.copy(hostError = "El host es obligatorio") }
            valid = false
        } else if (!s.host.trim().isValidHost()) {
            _uiState.update { it.copy(hostError = "Host inválido (dominio o IP)") }
            valid = false
        }
        if (s.port.isBlank() || !s.port.isValidPort()) {
            _uiState.update { it.copy(portError = "Puerto inválido (1–65535)") }
            valid = false
        }

        return valid
    }

    // ══════════════════════════════════════════════════════════════════════
    // GUARDAR
    // ══════════════════════════════════════════════════════════════════════

    fun save() {
        if (!validate()) return

        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val profile = VpnProfile(
                id         = s.profileId,
                name       = s.name.trim(),
                host       = s.host.trim(),
                port       = s.port.toIntOrNull() ?: 443,
                username   = s.username.trim(),
                password   = s.password,
                method     = s.method,
                sslEnabled = s.sslEnabled,
                sni        = s.sni.trim(),
                proxy      = ProxyConfig(
                    host = s.proxyHost.trim(),
                    port = s.proxyPort.toIntOrNull() ?: 0,
                    type = s.proxyType.trim()
                ),
                tagsRaw    = s.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(","),
                notes      = s.notes.trim(),
                enabled    = s.enabled
            )

            if (s.isEditMode) repository.updateProfile(profile)
            else repository.saveProfile(profile)

            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// UI STATE
// ══════════════════════════════════════════════════════════════════════════

data class CreateEditUiState(
    // Meta
    val isEditMode: Boolean       = false,
    val isLoading: Boolean        = true,
    val isSaving: Boolean         = false,
    val savedSuccessfully: Boolean = false,
    val error: String?            = null,

    // ID (para edición)
    val profileId: String         = VpnProfile.empty().id,

    // Campos del formulario
    val name: String              = "",
    val host: String              = "",
    val port: String              = "443",
    val username: String          = "",
    val password: String          = "",
    val method: String            = "ssh",
    val sslEnabled: Boolean       = true,
    val sni: String               = "",
    val proxyHost: String         = "",
    val proxyPort: String         = "",
    val proxyType: String         = "",
    val tags: String              = "",
    val notes: String             = "",
    val enabled: Boolean          = true,

    // UI helpers
    val passwordVisible: Boolean  = false,
    val showAdvanced: Boolean     = false,

    // Errores de validación
    val nameError: String?        = null,
    val hostError: String?        = null,
    val portError: String?        = null
) {
    val title: String get() = if (isEditMode) "Editar Perfil" else "Nuevo Perfil"
    val hasErrors: Boolean get() = nameError != null || hostError != null || portError != null
}

// Métodos de conexión disponibles
val VPN_METHODS = listOf("ssh", "v2ray", "shadowsocks", "wireguard", "trojan", "vless")
val PROXY_TYPES = listOf("", "http", "socks5")
