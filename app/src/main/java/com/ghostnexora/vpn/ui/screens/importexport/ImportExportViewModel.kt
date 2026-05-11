package com.ghostnexora.vpn.ui.screens.importexport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.data.repository.ProfileRepository
import com.ghostnexora.vpn.util.ImportResult
import com.ghostnexora.vpn.util.JsonManager
import com.ghostnexora.vpn.util.ValidationResult
import com.ghostnexora.vpn.util.shareFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════════════
// IMPORT / EXPORT VIEW MODEL
// ══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val jsonManager: JsonManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Import State ──────────────────────────────────────────────────────
    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    // ── Export State ──────────────────────────────────────────────────────
    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    // Perfiles disponibles para exportar
    val allProfiles: StateFlow<List<VpnProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ══════════════════════════════════════════════════════════════════════
    // IMPORTACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Carga y valida el archivo seleccionado por el usuario (SAF picker).
     * Actualiza el estado con la previsualización sin importar aún.
     */
    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            _importState.update { it.copy(isLoading = true, error = null) }

            try {
                // Leer nombre del archivo
                val fileName = resolveFileName(uri)

                // Validar y parsear
                val result = jsonManager.importFromUri(uri)

                when (result) {
                    is ImportResult.Success -> {
                        _importState.update {
                            it.copy(
                                isLoading       = false,
                                selectedUri     = uri,
                                fileName        = fileName,
                                previewProfiles = result.profiles,
                                sourceName      = result.sourceName,
                                validation      = ValidationResult(
                                    isValid      = true,
                                    message      = "Formato válido",
                                    profileCount = result.profiles.size
                                ),
                                error = null
                            )
                        }
                    }
                    is ImportResult.Error -> {
                        _importState.update {
                            it.copy(
                                isLoading   = false,
                                error       = result.message,
                                previewProfiles = emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _importState.update {
                    it.copy(isLoading = false, error = "Error inesperado: ${e.message}")
                }
            }
        }
    }

    /**
     * Confirma la importación con el modo seleccionado.
     * @param merge Si true, fusiona con los perfiles existentes.
     *              Si false, reemplaza todos los perfiles actuales.
     */
    fun confirmImport(merge: Boolean) {
        val profiles = _importState.value.previewProfiles
        if (profiles.isEmpty()) return

        viewModelScope.launch {
            _importState.update { it.copy(isLoading = true) }

            if (!merge) {
                // Reemplazar: borrar todo y reinsertar
                repository.deleteAllProfiles()
            }

            repository.saveProfiles(profiles)

            _importState.update {
                it.copy(
                    isLoading     = false,
                    importSuccess = true,
                    importedCount = profiles.size
                )
            }
        }
    }

    fun resetImport() {
        _importState.value = ImportUiState()
    }

    fun clearImportMessage() {
        _importState.update { it.copy(importSuccess = false, error = null) }
    }

    // ══════════════════════════════════════════════════════════════════════
    // EXPORTACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /** Alterna la selección de un perfil para exportar */
    fun toggleProfileSelection(profileId: String) {
        val current = _exportState.value.selectedIds.toMutableSet()
        if (profileId in current) current.remove(profileId) else current.add(profileId)
        _exportState.update { it.copy(selectedIds = current) }
    }

    /** Selecciona / deselecciona todos los perfiles */
    fun toggleSelectAll(profiles: List<VpnProfile>) {
        val allIds = profiles.map { it.id }.toSet()
        val current = _exportState.value.selectedIds
        _exportState.update {
            it.copy(selectedIds = if (current.size == allIds.size) emptySet() else allIds)
        }
    }

    /**
     * Exporta los perfiles seleccionados (o todos si ninguno seleccionado).
     * Abre el selector de compartir del sistema.
     */
    fun exportSelected(allProfiles: List<VpnProfile>) {
        viewModelScope.launch {
            _exportState.update { it.copy(isLoading = true, error = null) }

            val toExport = if (_exportState.value.selectedIds.isEmpty()) {
                allProfiles
            } else {
                allProfiles.filter { it.id in _exportState.value.selectedIds }
            }

            if (toExport.isEmpty()) {
                _exportState.update {
                    it.copy(isLoading = false, error = "No hay perfiles para exportar")
                }
                return@launch
            }

            val file = jsonManager.exportToFile(toExport)

            if (file != null) {
                context.shareFile(file)
                _exportState.update {
                    it.copy(
                        isLoading     = false,
                        exportSuccess = true,
                        exportedCount = toExport.size
                    )
                }
            } else {
                _exportState.update {
                    it.copy(isLoading = false, error = "Error al generar el archivo")
                }
            }
        }
    }

    fun clearExportMessage() {
        _exportState.update { it.copy(exportSuccess = false, error = null) }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private fun resolveFileName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: uri.lastPathSegment ?: "archivo.json"
        } catch (e: Exception) {
            "archivo.json"
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// UI STATES
// ══════════════════════════════════════════════════════════════════════════

data class ImportUiState(
    val isLoading: Boolean           = false,
    val selectedUri: Uri?            = null,
    val fileName: String             = "",
    val sourceName: String           = "",
    val previewProfiles: List<VpnProfile> = emptyList(),
    val validation: ValidationResult? = null,
    val importSuccess: Boolean       = false,
    val importedCount: Int           = 0,
    val error: String?               = null
) {
    val hasFile: Boolean    get() = selectedUri != null
    val canImport: Boolean  get() = previewProfiles.isNotEmpty() && !isLoading
}

data class ExportUiState(
    val isLoading: Boolean     = false,
    val selectedIds: Set<String> = emptySet(),
    val exportSuccess: Boolean = false,
    val exportedCount: Int     = 0,
    val error: String?         = null
) {
    val hasSelection: Boolean get() = selectedIds.isNotEmpty()
}
