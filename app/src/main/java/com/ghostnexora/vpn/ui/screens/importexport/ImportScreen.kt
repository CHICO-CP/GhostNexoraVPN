package com.ghostnexora.vpn.ui.screens.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════
// IMPORT SCREEN
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel()
) {
    val state by viewModel.importState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Diálogo de confirmación de importación
    var showMergeDialog by remember { mutableStateOf(false) }

    // File picker — solo JSON
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFilePicked(it) }
    }

    // Feedback de éxito
    LaunchedEffect(state.importSuccess) {
        if (state.importSuccess) {
            snackbarHostState.showSnackbar(
                "${state.importedCount} perfiles importados correctamente"
            )
            viewModel.clearImportMessage()
        }
    }

    // Feedback de error
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    // Diálogo merge/replace
    if (showMergeDialog) {
        ImportModeDialog(
            profileCount = state.previewProfiles.size,
            onMerge      = { showMergeDialog = false; viewModel.confirmImport(merge = true) },
            onReplace    = { showMergeDialog = false; viewModel.confirmImport(merge = false) },
            onDismiss    = { showMergeDialog = false }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary,
                    shape = SnackbarShape
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
        ) {

            // ── Header ──────────────────────────────────────────────────────
            item {
                ImportHeader()
            }

            // ── Zona de selección de archivo ────────────────────────────────
            item {
                FilePickerCard(
                    fileName  = state.fileName,
                    hasFile   = state.hasFile,
                    isLoading = state.isLoading,
                    onPick    = { filePicker.launch(arrayOf("application/json", "*/*")) },
                    onReset   = { viewModel.resetImport() }
                )
            }

            // ── Resultado de validación ──────────────────────────────────────
            if (state.validation != null) {
                item {
                    ValidationBanner(validation = state.validation!!)
                }
            }

            // ── Previsualización de perfiles ─────────────────────────────────
            if (state.previewProfiles.isNotEmpty()) {
                item {
                    Text(
                        text  = "VISTA PREVIA — ${state.previewProfiles.size} perfil(es)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary,
                            letterSpacing = 2.sp
                        )
                    )
                }

                items(state.previewProfiles) { profile ->
                    ImportProfilePreviewCard(profile = profile)
                }

                // ── Botones de acción ─────────────────────────────────────────
                item {
                    ImportActionButtons(
                        canImport = state.canImport,
                        onImport  = { showMergeDialog = true },
                        onCancel  = { viewModel.resetImport() }
                    )
                }
            }

            item { Spacer(Modifier.height(Dimens.Space3XL)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun ImportHeader() {
    GhostCard(
        borderColor     = NeonBlue.copy(alpha = 0.3f),
        glowColor       = NeonBlue,
        backgroundColor = NeonBlue.copy(alpha = 0.05f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(NeonBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(Dimens.IconLG)
                )
            }
            Column {
                Text(
                    text  = "Importar Perfiles",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text  = "Selecciona un archivo JSON con perfiles VPN",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ── Tarjeta de selección de archivo ──────────────────────────────────────

@Composable
private fun FilePickerCard(
    fileName: String,
    hasFile: Boolean,
    isLoading: Boolean,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    GhostCard(
        borderColor = if (hasFile) NeonCyan.copy(0.4f) else BorderSubtle
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            if (!hasFile) {
                // Estado vacío — zona de toque
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            width = Dimens.BorderNormal,
                            color = BorderNormal,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable(onClick = onPick)
                        .padding(Dimens.Space2XL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(Dimens.IconXL)
                        )
                        Text(
                            text  = "Toca para seleccionar archivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text  = "Formato soportado: .json",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                // Archivo seleccionado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(Dimens.IconMD)
                        )
                        Column {
                            Text(
                                text     = fileName,
                                style    = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text  = "Archivo seleccionado",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                    IconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Quitar archivo",
                            tint = TextTertiary
                        )
                    }
                }
            }

            // Loading
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = NeonCyan,
                    trackColor = SurfaceElevated
                )
            }
        }
    }
}

// ── Banner de validación ──────────────────────────────────────────────────

@Composable
private fun ValidationBanner(validation: com.ghostnexora.vpn.util.ValidationResult) {
    val (bg, border, icon, textColor) = if (validation.isValid) {
        listOf(NeonGreen.copy(0.06f), NeonGreen.copy(0.4f), Icons.Filled.CheckCircle, NeonGreen)
    } else {
        listOf(NeonRed.copy(0.06f), NeonRed.copy(0.4f), Icons.Filled.Error, NeonRed)
    }

    @Suppress("UNCHECKED_CAST")
    val (bgC, borderC, iconV, textC) = listOf(bg, border, icon, textColor)
        as List<Any>

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bgC as Color)
            .border(Dimens.BorderNormal, borderC as Color, MaterialTheme.shapes.medium)
            .padding(Dimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
    ) {
        Icon(
            imageVector = iconV as androidx.compose.ui.graphics.vector.ImageVector,
            contentDescription = null,
            tint = textC as Color,
            modifier = Modifier.size(Dimens.IconMD)
        )
        Column {
            Text(
                text  = validation.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textC,
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (validation.isValid) {
                Text(
                    text  = "${validation.profileCount} perfil(es) encontrado(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = textC.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Tarjeta de previsualización de perfil ─────────────────────────────────

@Composable
private fun ImportProfilePreviewCard(profile: VpnProfile) {
    GhostCard(
        borderColor     = BorderSubtle,
        contentPadding  = PaddingValues(Dimens.SpaceMD)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            // Indicador
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(NeonCyan.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(Dimens.IconSM)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = profile.name.ifEmpty { "Sin nombre" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = "${profile.host}:${profile.port}  ·  ${profile.method.uppercase()}",
                    style = MonoStyle.copy(fontSize = 11.sp, color = TextTertiary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // SSL badge
            if (profile.sslEnabled) {
                Box(
                    modifier = Modifier
                        .clip(TagShape)
                        .background(NeonGreen.copy(0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text  = "SSL",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen
                    )
                }
            }
        }
    }
}

// ── Botones de acción de importación ─────────────────────────────────────

@Composable
private fun ImportActionButtons(
    canImport: Boolean,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        GhostButton(
            text    = "Importar Perfiles",
            onClick = onImport,
            enabled = canImport,
            modifier = Modifier.fillMaxWidth()
        )
        GhostOutlineButton(
            text         = "Cancelar",
            onClick      = onCancel,
            borderColor  = BorderNormal,
            contentColor = TextSecondary,
            modifier     = Modifier.fillMaxWidth()
        )
    }
}

// ── Diálogo Merge / Replace ───────────────────────────────────────────────

@Composable
private fun ImportModeDialog(
    profileCount: Int,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceVariant,
        titleContentColor = TextPrimary,
        textContentColor  = TextSecondary,
        title = {
            Text(
                text  = "¿Cómo importar?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text = "Se van a importar $profileCount perfil(es). " +
                        "¿Quieres fusionarlos con los actuales o reemplazar todo?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                // Fusionar
                TextButton(onClick = onMerge) {
                    Text(
                        text  = "Fusionar",
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Reemplazar
                TextButton(onClick = onReplace) {
                    Text(
                        text  = "Reemplazar",
                        color = NeonRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
