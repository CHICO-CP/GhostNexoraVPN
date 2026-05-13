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

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    if (uiState.showClearDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearDialog,
            containerColor = SurfaceVariant, titleContentColor = TextPrimary, textContentColor = TextSecondary,
            icon = { Icon(Icons.Filled.Warning, null, tint = NeonRed, modifier = Modifier.size(Dimens.IconXL)) },
            title = { Text("Eliminar todos los datos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = { Text("Se eliminaran todos los perfiles VPN, registros y preferencias. Esta accion es irreversible.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(viewModel::confirmClearAll) { Text("Eliminar todo", color = NeonRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(viewModel::dismissClearDialog) { Text("Cancelar", color = TextSecondary) } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data, containerColor = SurfaceElevated, contentColor = TextPrimary, shape = SnackbarShape) } },
        containerColor = Color.Transparent
    ) { padding ->
        Column(Modifier.fillMaxSize().background(BackgroundDark).padding(padding).verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)) {
            Spacer(Modifier.height(Dimens.SpaceXS))
            SettingsSection("CONEXION", Icons.Filled.Wifi) {
                SettingsToggleItem(Icons.Filled.Autorenew, "Reconexion automatica", "Reconectar si se pierde la conexion", uiState.autoReconnect, viewModel::setAutoReconnect, NeonCyan)
                NeonDivider(Modifier.padding(horizontal = Dimens.SpaceSM))
                SettingsToggleItem(Icons.Filled.RestartAlt, "Reconectar al iniciar", "Conectar automaticamente al encender el dispositivo", uiState.reconnectOnBoot, viewModel::setReconnectOnBoot, NeonCyan)
            }
            SettingsSection("INTERFAZ", Icons.Filled.Tune) {
                SettingsToggleItem(Icons.Filled.Layers, "Ventana flotante", "Burbuja de control sobre otras apps", uiState.floatingWindow, viewModel::setFloatingWindow, NeonPurple)
                NeonDivider(Modifier.padding(horizontal = Dimens.SpaceSM))
                SettingsToggleItem(Icons.Filled.Notifications, "Notificaciones", "Mostrar estado VPN en la barra", uiState.notifications, viewModel::setNotifications, NeonBlue)
            }
            SettingsSection("REGISTROS", Icons.Filled.Terminal) {
                LogsMaxEntriesSelector(uiState.logsMaxEntries, viewModel::setLogsMaxEntries)
            }
            SettingsSection("PERMISOS", Icons.Filled.Security) {
                PermissionsStatusCard(uiState.permissionStatus, context)
            }
            SettingsSection("ZONA DE PELIGRO", Icons.Filled.Warning, NeonRed) {
                Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = viewModel::requestClearAll).padding(vertical = Dimens.SpaceXS), Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceMD)) {
                    Box(Modifier.size(36.dp).clip(MaterialTheme.shapes.small).background(NeonRed.copy(0.1f)), Alignment.Center) { Icon(Icons.Filled.DeleteForever, null, tint = NeonRed, modifier = Modifier.size(Dimens.IconSM)) }
                    Column(Modifier.weight(1f)) { Text("Eliminar todos los datos", style = MaterialTheme.typography.bodyMedium.copy(color = NeonRed, fontWeight = FontWeight.Medium)); Text("Borra perfiles, logs y preferencias", style = MaterialTheme.typography.bodySmall, color = TextTertiary) }
                    Icon(Icons.Filled.ChevronRight, null, tint = NeonRed.copy(0.5f), modifier = Modifier.size(Dimens.IconSM))
                }
            }
            Spacer(Modifier.height(Dimens.Space3XL))
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, accentColor: Color = NeonCyan, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(Dimens.IconSM))
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = accentColor, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            HorizontalDivider(Modifier.weight(2f), color = BorderSubtle, thickness = Dimens.BorderThin)
        }
        GhostCard { content() }
    }
}

@Composable
private fun SettingsToggleItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit, accentColor: Color = NeonCyan) {
    Row(Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceXS), Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceMD)) {
        Box(Modifier.size(36.dp).clip(MaterialTheme.shapes.small).background(accentColor.copy(0.1f)), Alignment.Center) { Icon(icon, null, tint = if (checked) accentColor else TextTertiary, modifier = Modifier.size(Dimens.IconSM)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(color = if (checked) TextPrimary else TextSecondary, fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Switch(checked, onToggle, colors = SwitchDefaults.colors(checkedThumbColor = TextOnAccent, checkedTrackColor = accentColor, uncheckedThumbColor = TextTertiary, uncheckedTrackColor = SurfaceElevated))
    }
}

@Composable
private fun LogsMaxEntriesSelector(current: Int, onSelect: (Int) -> Unit) {
    val options = listOf(100, 250, 500, 1000)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        Column { Text("Maximo de entradas", style = MaterialTheme.typography.bodyMedium, color = TextPrimary); Text("Logs mas antiguos se eliminan automaticamente", style = MaterialTheme.typography.bodySmall, color = TextTertiary) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            options.forEach { option ->
                val isSelected = option == current
                Box(Modifier.weight(1f).clip(MaterialTheme.shapes.small).background(if (isSelected) NeonCyan.copy(0.15f) else SurfaceElevated).border(Dimens.BorderNormal, if (isSelected) NeonCyan else BorderNormal, MaterialTheme.shapes.small).clickable { onSelect(option) }.padding(vertical = Dimens.SpaceSM), Alignment.Center) {
                    Text("$option", style = MaterialTheme.typography.labelMedium.copy(color = if (isSelected) NeonCyan else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                }
            }
        }
    }
}

@Composable
private fun PermissionsStatusCard(status: PermissionStatus, context: android.content.Context) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        PermissionRow("Permiso VPN", status.vpn, Icons.Filled.VpnKey, null)
        NeonDivider(Modifier.padding(horizontal = Dimens.SpaceXS))
        PermissionRow("Notificaciones", status.notification, Icons.Filled.Notifications) {
            context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra("android.provider.extra.APP_PACKAGE", context.packageName) })
        }
        NeonDivider(Modifier.padding(horizontal = Dimens.SpaceXS))
        PermissionRow("Ventana flotante (overlay)", status.overlay, Icons.Filled.Layers) { context.startActivity(PermissionHelper.overlayPermissionIntent(context)) }
        NeonDivider(Modifier.padding(horizontal = Dimens.SpaceXS))
        PermissionRow("Ignorar optimizacion bateria", status.battery, Icons.Filled.BatteryChargingFull) { context.startActivity(PermissionHelper.batteryOptimizationIntent(context)) }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, icon: ImageVector, onFix: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceXS), Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceSM)) {
        Icon(icon, null, tint = if (granted) NeonGreen else TextTertiary, modifier = Modifier.size(Dimens.IconSM))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = if (granted) TextSecondary else TextTertiary), modifier = Modifier.weight(1f))
        if (granted) Icon(Icons.Filled.CheckCircle, "Concedido", tint = NeonGreen, modifier = Modifier.size(Dimens.IconSM))
        else if (onFix != null) TextButton(onFix, contentPadding = PaddingValues(horizontal = Dimens.SpaceSM, vertical = 0.dp)) { Text("Conceder", style = MaterialTheme.typography.labelSmall, color = NeonAmber) }
    }
}
