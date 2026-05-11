package com.ghostnexora.vpn.ui.screens.logs

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import com.ghostnexora.vpn.data.model.LogEntry
import com.ghostnexora.vpn.data.model.LogLevel
import com.ghostnexora.vpn.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════
// LOGS SCREEN
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    val logs          by viewModel.logs.collectAsStateWithLifecycle()
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val searchQuery   by viewModel.searchQuery.collectAsStateWithLifecycle()
    val listState         = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-scroll al último log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(0)
    }

    // Snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Diálogo de limpieza
    if (uiState.showClearDialog) {
        ClearLogsDialog(
            count     = logs.size,
            onConfirm = viewModel::confirmClearLogs,
            onDismiss = viewModel::dismissClearDialog
        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
        ) {
            // ── Barra de búsqueda ──────────────────────────────────────────
            LogSearchBar(
                query    = searchQuery,
                onChange = viewModel::onSearchChange,
                onClear  = viewModel::clearSearch,
                onClearLogs = {
                    if (logs.isNotEmpty()) viewModel.requestClearLogs()
                }
            )

            // ── Filtros por nivel ──────────────────────────────────────────
            LevelFilterRow(
                selected = selectedLevel,
                onSelect = viewModel::setLevelFilter
            )

            // ── Contador ───────────────────────────────────────────────────
            LogCountBar(total = logs.size, filtered = selectedLevel != null || searchQuery.isNotBlank())

            // ── Lista de logs ──────────────────────────────────────────────
            if (logs.isEmpty()) {
                LogEmptyState(isFiltered = selectedLevel != null || searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(
                        horizontal = Dimens.ScreenPadding,
                        vertical   = Dimens.SpaceSM
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXS),
                    reverseLayout       = false
                ) {
                    items(logs, key = { it.id }) { entry ->
                        LogEntryRow(entry = entry)
                    }
                    item { Spacer(Modifier.height(Dimens.Space3XL)) }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════

// ── Barra de búsqueda + acción limpiar ───────────────────────────────────

@Composable
private fun LogSearchBar(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    onClearLogs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
    ) {
        OutlinedTextField(
            value         = query,
            onValueChange = onChange,
            placeholder   = { Text("Buscar en logs…", color = TextTertiary) },
            leadingIcon   = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (query.isNotBlank()) NeonCyan else TextTertiary
                )
            },
            trailingIcon = if (query.isNotBlank()) {
                { IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, null, tint = TextTertiary)
                }}
            } else null,
            singleLine    = true,
            modifier      = Modifier.weight(1f),
            shape         = InputFieldShape,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = NeonCyan,
                unfocusedBorderColor    = BorderNormal,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                cursorColor             = NeonCyan,
                focusedContainerColor   = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant
            )
        )

        // Botón limpiar todos los logs
        IconButton(
            onClick  = onClearLogs,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(NeonRed.copy(0.08f))
                .border(Dimens.BorderNormal, NeonRed.copy(0.3f), MaterialTheme.shapes.medium)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = "Limpiar logs",
                tint = NeonRed
            )
        }
    }
}

// ── Filtros de nivel ──────────────────────────────────────────────────────

@Composable
private fun LevelFilterRow(
    selected: LogLevel?,
    onSelect: (LogLevel?) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        modifier              = Modifier.padding(bottom = Dimens.SpaceSM)
    ) {
        // Chip "Todos"
        item {
            LevelChip(
                label      = "Todos",
                color      = NeonCyan,
                isSelected = selected == null,
                onClick    = { onSelect(null) }
            )
        }

        // Chips por nivel
        items(LogLevel.entries) { level ->
            val color = levelColor(level)
            LevelChip(
                label      = level.label,
                color      = color,
                isSelected = selected == level,
                onClick    = { onSelect(if (selected == level) null else level) }
            )
        }
    }
}

@Composable
private fun LevelChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(TagShape)
            .background(if (isSelected) color else color.copy(0.08f))
            .border(Dimens.BorderNormal, color.copy(if (isSelected) 1f else 0.3f), TagShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceXS),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = if (isSelected) TextOnAccent else color,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        )
    }
}

// ── Barra de contador ─────────────────────────────────────────────────────

@Composable
private fun LogCountBar(total: Int, filtered: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = if (filtered) "$total resultado(s) filtrado(s)" else "$total entrada(s) en total",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                letterSpacing = 0.5.sp
            )
        )
        // Indicador live
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(NeonGreen)
            )
            Text(
                text  = "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonGreen,
                    letterSpacing = 1.5.sp,
                    fontSize = 9.sp
                )
            )
        }
    }
}

// ── Entrada individual de log ─────────────────────────────────────────────

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val color = levelColor(entry.level)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = 0.04f))
            .border(
                Dimens.BorderThin,
                color.copy(alpha = 0.12f),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = Dimens.SpaceSM, vertical = Dimens.SpaceXS),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
    ) {
        // Timestamp
        Text(
            text  = entry.timeFormatted,
            style = LogEntryStyle.copy(
                color    = TextTertiary,
                fontSize = 10.sp
            ),
            modifier = Modifier.width(52.dp)
        )

        // Badge de nivel
        LogLevelBadge(
            level    = entry.level,
            modifier = Modifier.width(52.dp)
        )

        // Tag
        Text(
            text     = "[${entry.tag}]",
            style    = LogEntryStyle.copy(
                color    = NeonCyanDim,
                fontSize = 10.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp)
        )

        // Mensaje
        Text(
            text     = entry.message,
            style    = LogEntryStyle.copy(
                color    = color.copy(alpha = 0.85f),
                fontSize = 11.sp
            ),
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Estado vacío ──────────────────────────────────────────────────────────

@Composable
private fun LogEmptyState(isFiltered: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            Icon(
                imageVector = if (isFiltered) Icons.Filled.FilterAltOff
                              else Icons.Filled.Terminal,
                contentDescription = null,
                tint     = TextTertiary,
                modifier = Modifier.size(Dimens.Space4XL)
            )
            Text(
                text  = if (isFiltered) "Sin resultados con ese filtro"
                        else "Sin registros aún",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text  = if (isFiltered) "Prueba con otro nivel o búsqueda"
                        else "Los logs aparecerán al conectar la VPN",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

// ── Diálogo de limpieza ───────────────────────────────────────────────────

@Composable
private fun ClearLogsDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = SurfaceVariant,
        titleContentColor = TextPrimary,
        textContentColor  = TextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = null,
                tint     = NeonRed,
                modifier = Modifier.size(Dimens.IconXL)
            )
        },
        title = { Text("Limpiar logs", fontWeight = FontWeight.Bold) },
        text  = {
            Text(
                "¿Eliminar las $count entrada(s) del registro? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Limpiar", color = NeonRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

// ── Helper: color por nivel ───────────────────────────────────────────────

fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.DEBUG   -> TextTertiary
    LogLevel.INFO    -> NeonBlue
    LogLevel.SUCCESS -> NeonGreen
    LogLevel.WARNING -> NeonAmber
    LogLevel.ERROR   -> NeonRed
}
