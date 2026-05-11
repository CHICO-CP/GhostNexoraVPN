package com.ghostnexora.vpn.ui.screens.importexport

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
// EXPORT SCREEN
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel()
) {
    val state    by viewModel.exportState.collectAsStateWithLifecycle()
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Feedback de éxito/error
    LaunchedEffect(state.exportSuccess) {
        if (state.exportSuccess) {
            snackbarHostState.showSnackbar("${state.exportedCount} perfil(es) exportado(s)")
            viewModel.clearExportMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = SurfaceElevated,
                    contentColor   = TextPrimary,
                    shape          = SnackbarShape
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
            item { ExportHeader() }

            // ── Sin perfiles ────────────────────────────────────────────────
            if (profiles.isEmpty()) {
                item { ExportEmptyState() }
            } else {

                // ── Barra de selección ───────────────────────────────────────
                item {
                    SelectionBar(
                        totalCount    = profiles.size,
                        selectedCount = state.selectedIds.size,
                        onSelectAll   = { viewModel.toggleSelectAll(profiles) }
                    )
                }

                // ── Lista de perfiles ────────────────────────────────────────
                items(profiles, key = { it.id }) { profile ->
                    ExportProfileCard(
                        profile    = profile,
                        isSelected = profile.id in state.selectedIds,
                        onToggle   = { viewModel.toggleProfileSelection(profile.id) }
                    )
                }

                // ── Botón de exportar ────────────────────────────────────────
                item {
                    ExportActionButton(
                        selectedCount = state.selectedIds.size,
                        totalCount    = profiles.size,
                        isLoading     = state.isLoading,
                        onClick       = { viewModel.exportSelected(profiles) }
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
private fun ExportHeader() {
    GhostCard(
        borderColor     = NeonAmber.copy(alpha = 0.3f),
        glowColor       = NeonAmber,
        backgroundColor = NeonAmber.copy(alpha = 0.05f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(NeonAmber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(Dimens.IconLG)
                )
            }
            Column {
                Text(
                    text  = "Exportar Perfiles",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text  = "Selecciona los perfiles a exportar como JSON",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ── Barra de selección ────────────────────────────────────────────────────

@Composable
private fun SelectionBar(
    totalCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = when {
                selectedCount == 0          -> "Ninguno seleccionado (se exportarán todos)"
                selectedCount == totalCount -> "Todos seleccionados"
                else                        -> "$selectedCount de $totalCount seleccionados"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        TextButton(onClick = onSelectAll) {
            Text(
                text  = if (selectedCount == totalCount) "Deseleccionar todo" else "Seleccionar todo",
                color = NeonCyan,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// ── Tarjeta de perfil para exportar ──────────────────────────────────────

@Composable
private fun ExportProfileCard(
    profile: VpnProfile,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (isSelected) NeonCyan.copy(0.5f) else BorderSubtle
    val bgColor     = if (isSelected) NeonCyan.copy(0.05f) else SurfaceVariant

    GhostCard(
        borderColor     = borderColor,
        backgroundColor = bgColor,
        contentPadding  = PaddingValues(Dimens.SpaceMD),
        modifier        = Modifier.clickable(onClick = onToggle)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            // Checkbox visual
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (isSelected) NeonCyan else SurfaceElevated)
                    .border(
                        Dimens.BorderNormal,
                        if (isSelected) NeonCyan else BorderNormal,
                        MaterialTheme.shapes.extraSmall
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter   = scaleIn(initialScale = 0.5f),
                    exit    = scaleOut(targetScale = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = TextOnAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Ícono del perfil
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(NeonCyan.copy(0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = null,
                    tint = if (isSelected) NeonCyan else TextTertiary,
                    modifier = Modifier.size(Dimens.IconSM)
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = profile.name.ifEmpty { "Sin nombre" },
                    style    = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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

            // Tags count
            if (profile.tags.isNotEmpty()) {
                Text(
                    text  = "${profile.tags.size} tag(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

// ── Botón de exportar ─────────────────────────────────────────────────────

@Composable
private fun ExportActionButton(
    selectedCount: Int,
    totalCount: Int,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val label = when {
        selectedCount == 0 -> "Exportar todos ($totalCount)"
        selectedCount == 1 -> "Exportar 1 perfil"
        else               -> "Exportar $selectedCount perfiles"
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier   = Modifier.fillMaxWidth(),
                color      = NeonAmber,
                trackColor = SurfaceElevated
            )
        }

        GhostButton(
            text           = label,
            onClick        = onClick,
            enabled        = !isLoading && totalCount > 0,
            containerColor = NeonAmber,
            contentColor   = TextOnAccent,
            modifier       = Modifier.fillMaxWidth()
        )

        // Nota informativa
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(Dimens.IconXS)
            )
            Text(
                text  = "El archivo JSON se compartirá mediante el sistema de Android",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

// ── Estado vacío ──────────────────────────────────────────────────────────

@Composable
private fun ExportEmptyState() {
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
                imageVector = Icons.Filled.FolderOff,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(Dimens.Space4XL)
            )
            Text(
                text  = "No hay perfiles para exportar",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text  = "Crea o importa perfiles primero",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}
