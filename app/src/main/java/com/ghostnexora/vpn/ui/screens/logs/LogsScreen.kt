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

@Composable
fun LogsScreen(viewModel: LogsViewModel = hiltViewModel()) {
    val logs          by viewModel.logs.collectAsStateWithLifecycle()
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val searchQuery   by viewModel.searchQuery.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(0) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.showClearDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearDialog,
            containerColor = SurfaceVariant, titleContentColor = TextPrimary, textContentColor = TextSecondary,
            icon = { Icon(Icons.Filled.DeleteSweep, null, tint = NeonRed, modifier = Modifier.size(Dimens.IconXL)) },
            title = { Text("Limpiar logs", fontWeight = FontWeight.Bold) },
            text = { Text("Eliminar las ${logs.size} entrada(s) del registro? Esta accion no se puede deshacer.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(viewModel::confirmClearLogs) { Text("Limpiar", color = NeonRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(viewModel::dismissClearDialog) { Text("Cancelar", color = TextSecondary) } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data, containerColor = SurfaceElevated, contentColor = TextPrimary, shape = SnackbarShape) } },
        containerColor = Color.Transparent
    ) { padding ->
        Column(Modifier.fillMaxSize().background(BackgroundDark).padding(padding)) {
            // Search bar
            Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSM), Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceSM)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = viewModel::onSearchChange,
                    placeholder = { Text("Buscar en logs...", color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = if (searchQuery.isNotBlank()) NeonCyan else TextTertiary) },
                    trailingIcon = if (searchQuery.isNotBlank()) { { IconButton(viewModel::clearSearch) { Icon(Icons.Filled.Close, null, tint = TextTertiary) } } } else null,
                    singleLine = true, modifier = Modifier.weight(1f), shape = InputFieldShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = BorderNormal, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonCyan, focusedContainerColor = SurfaceVariant, unfocusedContainerColor = SurfaceVariant)
                )
                IconButton(onClick = { if (logs.isNotEmpty()) viewModel.requestClearLogs() }, modifier = Modifier.clip(MaterialTheme.shapes.medium).background(NeonRed.copy(0.08f)).border(Dimens.BorderNormal, NeonRed.copy(0.3f), MaterialTheme.shapes.medium)) {
                    Icon(Icons.Filled.DeleteSweep, "Limpiar logs", tint = NeonRed)
                }
            }
            // Level filters
            LazyRow(contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM), modifier = Modifier.padding(bottom = Dimens.SpaceSM)) {
                item { LevelChip("Todos", NeonCyan, selectedLevel == null) { viewModel.setLevelFilter(null) } }
                items(items = LogLevel.entries) { level ->
                    LevelChip(level.label, levelColor(level), selectedLevel == level) { viewModel.setLevelFilter(if (selectedLevel == level) null else level) }
                }
            }
            // Counter
            Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceXS), Alignment.CenterVertically, Arrangement.SpaceBetween) {
                Text(if (selectedLevel != null || searchQuery.isNotBlank()) "${logs.size} resultado(s)" else "${logs.size} entrada(s)", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
                    Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(NeonGreen))
                    Text("LIVE", style = MaterialTheme.typography.labelSmall.copy(color = NeonGreen, letterSpacing = 1.5.sp, fontSize = 9.sp))
                }
            }
            // List
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(Alignment.CenterHorizontally, Arrangement.spacedBy(Dimens.SpaceMD)) {
                        Icon(if (selectedLevel != null || searchQuery.isNotBlank()) Icons.Filled.FilterAltOff else Icons.Filled.Terminal, null, tint = TextTertiary, modifier = Modifier.size(Dimens.Space4XL))
                        Text(if (selectedLevel != null || searchQuery.isNotBlank()) "Sin resultados con ese filtro" else "Sin registros aun", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(if (selectedLevel != null || searchQuery.isNotBlank()) "Prueba con otro nivel o busqueda" else "Los logs apareceran al conectar la VPN", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXS)) {
                    items(items = logs, key = { entry -> entry.id }) { entry -> LogEntryRow(entry) }
                    item { Spacer(Modifier.height(Dimens.Space3XL)) }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(TagShape).background(if (isSelected) color else color.copy(0.08f)).border(Dimens.BorderNormal, color.copy(if (isSelected) 1f else 0.3f), TagShape).clickable(onClick = onClick).padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceXS), Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) TextOnAccent else color, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, letterSpacing = 0.5.sp))
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val color = levelColor(entry.level)
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraSmall).background(color.copy(0.04f)).border(Dimens.BorderThin, color.copy(0.12f), MaterialTheme.shapes.extraSmall).padding(horizontal = Dimens.SpaceSM, vertical = Dimens.SpaceXS), Alignment.Top, Arrangement.spacedBy(Dimens.SpaceSM)) {
        Text(entry.timeFormatted, style = LogEntryStyle.copy(color = TextTertiary, fontSize = 10.sp), modifier = Modifier.width(52.dp))
        LogLevelBadge(entry.level, Modifier.width(52.dp))
        Text("[${entry.tag}]", style = LogEntryStyle.copy(color = NeonCyanDim, fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(60.dp))
        Text(entry.message, style = LogEntryStyle.copy(color = color.copy(0.85f), fontSize = 11.sp), modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.DEBUG   -> TextTertiary
    LogLevel.INFO    -> NeonBlue
    LogLevel.SUCCESS -> NeonGreen
    LogLevel.WARNING -> NeonAmber
    LogLevel.ERROR   -> NeonRed
}
