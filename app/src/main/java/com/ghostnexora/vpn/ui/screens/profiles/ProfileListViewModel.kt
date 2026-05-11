package com.ghostnexora.vpn.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    // ── Búsqueda ──────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ── Filtro activo ─────────────────────────────────────────────────────
    private val _activeFilter = MutableStateFlow(ProfileFilter.ALL)
    val activeFilter: StateFlow<ProfileFilter> = _activeFilter.asStateFlow()

    // ── UI State ──────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(ProfileListUiState())
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()

    // ── Perfil activo (desde DataStore) ───────────────────────────────────
    val activeProfileId: StateFlow<String> = repository.activeProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // ══════════════════════════════════════════════════════════════════════
    // LISTA REACTIVA DE PERFILES
    // Combina búsqueda + filtro en un solo Flow
    // ══════════════════════════════════════════════════════════════════════

    @OptIn(ExperimentalCoroutinesApi::class)
    val profiles: StateFlow<List<VpnProfile>> = combine(
        _searchQuery,
        _activeFilter
    ) { query, filter -> Pair(query, filter) }
        .flatMapLatest { (query, filter) ->
            when {
                query.isNotBlank() -> repository.searchProfiles(query)
                filter == ProfileFilter.FAVORITES -> repository.favoriteProfiles
                filter == ProfileFilter.ENABLED   -> repository.enabledProfiles
                else                              -> repository.allProfiles
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ══════════════════════════════════════════════════════════════════════
    // ACCIONES
    // ══════════════════════════════════════════════════════════════════════

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setFilter(filter: ProfileFilter) {
        _activeFilter.value = filter
    }

    /** Selecciona el perfil como activo en DataStore */
    fun selectActiveProfile(profileId: String) {
        viewModelScope.launch {
            repository.setActiveProfileId(profileId)
            _uiState.update { it.copy(snackbarMessage = "Perfil seleccionado") }
        }
    }

    /** Alterna favorito */
    fun toggleFavorite(profile: VpnProfile) {
        viewModelScope.launch {
            repository.setFavorite(profile.id, !profile.isFavorite)
        }
    }

    /** Solicita confirmación de borrado */
    fun requestDelete(profile: VpnProfile) {
        _uiState.update { it.copy(profileToDelete = profile) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(profileToDelete = null) }
    }

    /** Elimina el perfil pendiente */
    fun confirmDelete() {
        val profile = _uiState.value.profileToDelete ?: return
        viewModelScope.launch {
            repository.deleteProfile(profile)
            _uiState.update {
                it.copy(
                    profileToDelete = null,
                    snackbarMessage = "\"${profile.name}\" eliminado"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// MODELOS DE ESTADO
// ══════════════════════════════════════════════════════════════════════════

data class ProfileListUiState(
    val profileToDelete: VpnProfile? = null,
    val snackbarMessage: String?     = null
)

enum class ProfileFilter(val label: String) {
    ALL("Todos"),
    FAVORITES("Favoritos"),
    ENABLED("Activos")
}
