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
import com.ghostnexora.vpn.util.ValidationResult

@Composable
fun ImportScreen(onBack: () -> Unit, viewModel: ImportExportViewModel = hiltViewModel()) {
    val state = viewModel.importState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    var showMergeDialog by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.onFilePicked(it) } }

    LaunchedEffect(state.importSuccess) {
        if (state.importSuccess) { snackbarHostState.showSnackbar(message = "${state.importedCount} perfiles importados correctamente"); viewModel.clearImportMessage() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { msg -> snackbarHostState.showSnackbar(message = msg); viewModel.clearImportMessage() }
    }

    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            containerColor = SurfaceVariant, titleContentColor = TextPrimary, textContentColor = TextSecondary,
            title = { Text("Como importar?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = { Text("Se van a importar ${state.previewProfiles.size} perfil(es). Fusionar con los actuales o reemplazar todo?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                    TextButton(onClick = { showMergeDialog = false; viewModel.confirmImport(true) }) { Text("Fusionar", color = NeonCyan, fontWeight = FontWeight.SemiBold) }
                    TextButton(onClick = { showMergeDialog = false; viewModel.confirmImport(false) }) { Text("Reemplazar", color = NeonRed, fontWeight = FontWeight.SemiBold) }
                }
            },
            dismissButton = { TextButton({ showMergeDialog = false }) { Text("Cancelar", color = TextSecondary) } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data, containerColor = SurfaceElevated, contentColor = TextPrimary, shape = SnackbarShape) } },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(BackgroundDark).padding(padding).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
        ) {
            // Header
            item {
                GhostCard(NeonBlue.copy(0.3f), NeonBlue, NeonBlue.copy(0.05f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                        Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(NeonBlue.copy(0.15f)), Alignment.Center) { Icon(Icons.Filled.FileDownload, null, tint = NeonBlue, modifier = Modifier.size(Dimens.IconLG)) }
                        Column { Text("Importar Perfiles", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)); Text("Selecciona un archivo JSON con perfiles VPN", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                    }
                }
            }
            // File picker
            item {
                GhostCard(if (state.hasFile) NeonCyan.copy(0.4f) else BorderSubtle) {
                    Column(Modifier.fillMaxWidth(), Alignment.CenterHorizontally, Arrangement.spacedBy(Dimens.SpaceMD)) {
                        if (!state.hasFile) {
                            Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).border(Dimens.BorderNormal, BorderNormal, MaterialTheme.shapes.medium).clickable { filePicker.launch(arrayOf("application/json", "*/*")) }.padding(Dimens.Space2XL), Alignment.Center) {
                                Column(Alignment.CenterHorizontally, Arrangement.spacedBy(Dimens.SpaceSM)) {
                                    Icon(Icons.Filled.FolderOpen, null, tint = NeonCyan, modifier = Modifier.size(Dimens.IconXL))
                                    Text("Toca para seleccionar archivo", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Text("Formato soportado: .json", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                }
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), Alignment.CenterVertically, Arrangement.SpaceBetween) {
                                Row(Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceSM), Modifier.weight(1f)) {
                                    Icon(Icons.Filled.InsertDriveFile, null, tint = NeonCyan, modifier = Modifier.size(Dimens.IconMD))
                                    Column { Text(state.fileName, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Archivo seleccionado", style = MaterialTheme.typography.bodySmall, color = NeonCyan) }
                                }
                                IconButton(viewModel::resetImport) { Icon(Icons.Filled.Close, "Quitar", tint = TextTertiary) }
                            }
                        }
                        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = NeonCyan, trackColor = SurfaceElevated)
                    }
                }
            }
            // Validation
            if (state.validation != null) {
                item {
                    val isValid = state.validation!!.isValid
                    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(if (isValid) NeonGreen.copy(0.06f) else NeonRed.copy(0.06f)).border(Dimens.BorderNormal, if (isValid) NeonGreen.copy(0.4f) else NeonRed.copy(0.4f), MaterialTheme.shapes.medium).padding(Dimens.SpaceMD), Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceSM)) {
                        Icon(if (isValid) Icons.Filled.CheckCircle else Icons.Filled.Error, null, tint = if (isValid) NeonGreen else NeonRed, modifier = Modifier.size(Dimens.IconMD))
                        Column { Text(state.validation!!.message, style = MaterialTheme.typography.bodyMedium.copy(color = if (isValid) NeonGreen else NeonRed, fontWeight = FontWeight.SemiBold)); if (isValid) Text("${state.validation!!.profileCount} perfil(es) encontrado(s)", style = MaterialTheme.typography.bodySmall, color = if (isValid) NeonGreen.copy(0.7f) else NeonRed.copy(0.7f)) }
                    }
                }
            }
            // Preview
            if (state.previewProfiles.isNotEmpty()) {
                item { Text("VISTA PREVIA - ${state.previewProfiles.size} perfil(es)", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, letterSpacing = 2.sp)) }
                items(items = state.previewProfiles, key = { profile -> profile.id }) { profile ->
                    GhostCard(BorderSubtle, contentPadding = PaddingValues(Dimens.SpaceMD)) {
                        Row(Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceMD)) {
                            Box(Modifier.size(36.dp).clip(MaterialTheme.shapes.small).background(NeonCyan.copy(0.1f)), Alignment.Center) { Icon(Icons.Filled.VpnKey, null, tint = NeonCyan, modifier = Modifier.size(Dimens.IconSM)) }
                            Column(Modifier.weight(1f)) {
                                Text(profile.name.ifEmpty { "Sin nombre" }, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${profile.host}:${profile.port}  .  ${profile.method.uppercase()}", style = MonoStyle.copy(11.sp, color = TextTertiary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (profile.sslEnabled) Box(Modifier.clip(TagShape).background(NeonGreen.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("SSL", style = MaterialTheme.typography.labelSmall, color = NeonGreen) }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                        GhostButton("Importar Perfiles", { showMergeDialog = true }, Modifier.fillMaxWidth(), enabled = state.canImport)
                        GhostOutlineButton("Cancelar", viewModel::resetImport, Modifier.fillMaxWidth(), borderColor = BorderNormal, contentColor = TextSecondary)
                    }
                }
            }
            item { Spacer(Modifier.height(Dimens.Space3XL)) }
        }
    }
}
