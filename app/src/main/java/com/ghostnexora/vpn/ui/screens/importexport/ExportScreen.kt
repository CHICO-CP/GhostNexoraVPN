package com.ghostnexora.vpn.ui.screens.importexport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.ui.theme.*

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel()
) {
    val state = viewModel.exportState.collectAsStateWithLifecycle().value
    val profiles = viewModel.allProfiles.collectAsStateWithLifecycle().value

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar mensajes de éxito
    LaunchedEffect(state.exportSuccess) {
        if (state.exportSuccess) {
            snackbarHostState.showSnackbar(
                message = "${state.exportedCount} perfil(es) exportado(s)"
            )
            viewModel.clearExportMessage()
        }
    }

    // Mostrar errores
    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Exportar Perfiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
        ) {
            // Header
            item {
                GhostCard(
                    backgroundColor = NeonAmber.copy(0.3f),
                    borderColor = NeonAmber,
                    contentColor = NeonAmber.copy(0.05f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(NeonAmber.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.FileUpload,
                                null,
                                tint = NeonAmber,
                                modifier = Modifier.size(Dimens.IconLG)
                            )
                        }
                        Column {
                            Text(
                                "Exportar Perfiles",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                "Selecciona los perfiles a exportar como JSON",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (profiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.Space3XL),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
                        ) {
                            Icon(
                                Icons.Filled.FolderOff,
                                null,
                                tint = TextTertiary,
                                modifier = Modifier.size(Dimens.Space4XL)
                            )
                            Text(
                                "No hay perfiles para exportar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                "Crea o importa perfiles primero",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            } else {
                // Selector
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                state.selectedIds.isEmpty() -> "Ninguno seleccionado (se exportarán todos)"
                                state.selectedIds.size == profiles.size -> "Todos seleccionados"
                                else -> "${state.selectedIds.size} de ${profiles.size} seleccionados"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        TextButton(onClick = { viewModel.toggleSelectAll(profiles) }) {
                            Text(
                                text = if (state.selectedIds.size == profiles.size) 
                                    "Deseleccionar todo" 
                                else "Seleccionar todo",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // Lista de perfiles
                items(
                    items = profiles,
                    key = { it.id }
                ) { profile ->
                    val isSelected = profile.id in state.selectedIds

                    GhostCard(
                        backgroundColor = if (isSelected) NeonCyan.copy(0.5f) else BorderSubtle,
                        borderColor = null,
                        contentColor = if (isSelected) NeonCyan.copy(0.05f) else SurfaceVariant,
                        padding = PaddingValues(Dimens.SpaceMD),
                        modifier = Modifier.clickable {
                            viewModel.toggleProfileSelection(profile.id)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
                        ) {
                            // Checkbox
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(if (isSelected) NeonCyan else SurfaceElevated)
                                    .border(
                                        width = Dimens.BorderNormal,
                                        color = if (isSelected) NeonCyan else BorderNormal,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        null,
                                        tint = TextOnAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Icono VPN
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(NeonCyan.copy(0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.VpnKey,
                                    null,
                                    tint = if (isSelected) NeonCyan else TextTertiary,
                                    modifier = Modifier.size(Dimens.IconSM)
                                )
                            }

                            // Información del perfil
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = profile.name.ifEmpty { "Sin nombre" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "\( {profile.host}: \){profile.port}  •  ${profile.method.uppercase()}",
                                    style = MonoStyle.copy(fontSize = 11.sp, color = TextTertiary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Tags
                            if (profile.tags.isNotEmpty()) {
                                Text(
                                    text = "${profile.tags.size} tag(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }

                // Botón de exportar
                item {
                    val label = when {
                        state.selectedIds.isEmpty() -> "Exportar todos (${profiles.size})"
                        state.selectedIds.size == 1 -> "Exportar 1 perfil"
                        else -> "Exportar ${state.selectedIds.size} perfiles"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                        if (state.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = NeonAmber,
                                trackColor = SurfaceElevated
                            )
                        }

                        GhostButton(
                            text = label,
                            onClick = { viewModel.exportSelected(profiles) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading && profiles.isNotEmpty(),
                            containerColor = NeonAmber,
                            contentColor = TextOnAccent
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                null,
                                tint = TextTertiary,
                                modifier = Modifier.size(Dimens.IconXS)
                            )
                            Text(
                                "El archivo JSON se compartirá mediante el sistema de Android",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(Dimens.Space3XL)) }
        }
    }
}