package com.ghostnexora.vpn.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.ui.theme.*
import com.ghostnexora.vpn.util.PermissionHelper
import com.ghostnexora.vpn.util.PermissionStatus

// ══════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Refrescar permisos al volver a la pantalla
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    // Diálogo de limpiar datos
    if (uiState.showClearDialog) {
        ClearDataDialog(
            onConfirm = viewModel::confirmClearAll,
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
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
        ) {
            Spacer(Modifier.height(Dimens.SpaceXS))

            // ── 1. Sección de conexión ─────────────────────────────────────
            SettingsSection(title = "CONEXIÓN", icon = Icons.Filled.Wifi) {
                SettingsToggleItem(
                    icon        = Icons.Filled.Autorenew,
                    title       = "Reconexión automática",
                    subtitle    = "Reconectar si se pierde la conexión",
                    checked     = uiState.autoReconnect,
                    onToggle    = viewModel::setAutoReconnect,
                    accentColor = NeonCyan
                )
                NeonDivider(modifier = Modifier.padding(horizontal = Dimens.SpaceSM))
                SettingsToggleItem(
                    icon        = Icons.Filled.RestartAlt,
                    title       = "Reconectar al iniciar",
                    subtitle    = "Conectar automáticamente al encender el dispositivo",
                    checked     = uiState.reconnectOnBoot,
                    onToggle    = viewModel::setReconnectOnBoot,
                    accentColor = NeonCyan
                )
            }

            // ── 2. Sección de interfaz ─────────────────────────────────────
            SettingsSection(title = "INTERFAZ", icon = Icons.Filled.Tune) {
                SettingsToggleItem(
                    icon        = Icons.Filled.Layers,
                    title       = "Ventana flotante",
                    subtitle    = "Burbuja de control sobre otras apps",
                    checked     = uiState.floatingWindow,
                    onToggle    = viewModel::setFloatingWindow,
                    accentColor = NeonPurple
                )
                NeonDivider(modifier = Modifier.padding(horizontal = Dimens.SpaceSM))
                SettingsToggleItem(
                    icon        = Icons.Filled.Notifications,
                    title       = "Notificaciones",
                    subtitle    = "Mostrar estado VPN en la barra de notificaciones",
                    checked     = uiState.notifications,
                    onToggle    = viewModel::setNotifications,
                    accentColor = NeonBlue
                )
            }

            // ── 3. Sección de logs ─────────────────────────────────────────
            SettingsSection(title = "REGISTROS", icon = Icons.Filled.Terminal) {
                LogsMaxEntriesSelector(
                    current  = uiState.logsMaxEntries,
                    onSelect = viewModel::setLogsMaxEntries
                )
            }

            // ── 4. Estado de permisos ──────────────────────────────────────
            SettingsSection(title = "PERMISOS", icon = Icons.Filled.Security) {
                PermissionsStatusCard(
                    status  = uiState.permissionStatus,
                    context = context
                )
            }

            // ── 5. Zona de peligro ─────────────────────────────────────────
            SettingsSection(
                title       = "ZONA DE PELIGRO",
                icon        = Icons.Filled.Warning,
                accentColor = NeonRed
            ) {
                SettingsActionItem(
                    icon        = Icons.Filled.DeleteForever,
                    title       = "Eliminar todos los datos",
                    subtitle    = "Borra perfiles, logs y preferencias",
                    color       = NeonRed,
                    onClick     = viewModel::requestClearAll
                )
            }

            Spacer(Modifier.height(Dimens.Space3XL))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════

// ── Sección con encabezado ────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    accentColor: Color = NeonCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(Dimens.IconSM)
            )
            Text(
                text  = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color         = accentColor,
                    letterSpacing = 2.sp,
                    fontWeight    = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.weight(1f))
            HorizontalDivider(
                modifier  = Modifier.weight(2f),
                color     = BorderSubtle,
                thickness = Dimens.BorderThin
            )
        }

        GhostCard {
            content()
        }
    }
}

// ── Item con toggle ───────────────────────────────────────────────────────

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: Color = NeonCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(accentColor.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) accentColor else TextTertiary,
                modifier = Modifier.size(Dimens.IconSM)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color      = if (checked) TextPrimary else TextSecondary,
                    fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
                )
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }

        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = TextOnAccent,
                checkedTrackColor   = accentColor,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = SurfaceElevated
            )
        )
    }
}

// ── Item de acción ────────────────────────────────────────────────────────

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.SpaceXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Dimens.IconSM)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color      = color,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = color.copy(0.5f),
            modifier = Modifier.size(Dimens.IconSM)
        )
    }
}

// ── Selector de máximo de logs ────────────────────────────────────────────

@Composable
private fun LogsMaxEntriesSelector(
    current: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(100, 250, 500, 1000)

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text  = "Máximo de entradas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text  = "Logs más antiguos se eliminan automáticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val isSelected = option == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isSelected) NeonCyan.copy(0.15f) else SurfaceElevated
                        )
                        .border(
                            Dimens.BorderNormal,
                            if (isSelected) NeonCyan else BorderNormal,
                            MaterialTheme.shapes.small
                        )
                        .clickable { onSelect(option) }
                        .padding(vertical = Dimens.SpaceSM),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "$option",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color      = if (isSelected) NeonCyan else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

// ── Tarjeta de estado de permisos ─────────────────────────────────────────

@Composable
private fun PermissionsStatusCard(
    status: PermissionStatus,
    context: android.content.Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        // VPN
        PermissionRow(
            label   = "Permiso VPN",
            granted = status.vpn,
            icon    = Icons.Filled.VpnKey,
            onFix   = null // Se solicita en el Dashboard al conectar
        )
        NeonDivider(modifier = Modifier.padding(horizontal = Dimens.SpaceXS))

        // Notificaciones
        PermissionRow(
            label   = "Notificaciones",
            granted = status.notification,
            icon    = Icons.Filled.Notifications,
            onFix   = {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                    }
                )
            }
        )
        NeonDivider(modifier = Modifier.padding(horizontal = Dimens.SpaceXS))

        // Overlay
        PermissionRow(
            label   = "Ventana flotante (overlay)",
            granted = status.overlay,
            icon    = Icons.Filled.Layers,
            onFix   = {
                context.startActivity(
                    PermissionHelper.overlayPermissionIntent(context)
                )
            }
        )
        NeonDivider(modifier = Modifier.padding(horizontal = Dimens.SpaceXS))

        // Batería
        PermissionRow(
            label   = "Ignorar optimización batería",
            granted = status.battery,
            icon    = Icons.Filled.BatteryChargingFull,
            onFix   = {
                context.startActivity(
                    PermissionHelper.batteryOptimizationIntent(context)
                )
            }
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    icon: ImageVector,
    onFix: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) NeonGreen else TextTertiary,
            modifier = Modifier.size(Dimens.IconSM)
        )
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall.copy(
                color = if (granted) TextSecondary else TextTertiary
            ),
            modifier = Modifier.weight(1f)
        )
        if (granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Concedido",
                tint = NeonGreen,
                modifier = Modifier.size(Dimens.IconSM)
            )
        } else if (onFix != null) {
            TextButton(
                onClick = onFix,
                contentPadding = PaddingValues(horizontal = Dimens.SpaceSM, vertical = 0.dp)
            ) {
                Text(
                    text  = "Conceder",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonAmber
                )
            }
        }
    }
}

// ── Diálogo de limpiar datos ──────────────────────────────────────────────

@Composable
private fun ClearDataDialog(
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
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = NeonRed,
                modifier = Modifier.size(Dimens.IconXL)
            )
        },
        title = {
            Text(
                text  = "Eliminar todos los datos",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text  = "Se eliminarán todos los perfiles VPN, registros y preferencias. " +
                        "Esta acción es irreversible.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text  = "Eliminar todo",
                    color = NeonRed,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
