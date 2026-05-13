package com.ghostnexora.vpn.ui.screens.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.data.model.VpnConnectionState
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.ui.theme.*
import com.ghostnexora.vpn.util.toSessionTime

@Composable
fun DashboardScreen(
    onNavigateToProfiles: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val snackbarHostState = remember { SnackbarHostState() }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onVpnPermissionGranted()
        else viewModel.onVpnPermissionDenied()
    }

    LaunchedEffect(uiState.pendingVpnPermissionIntent) {
        uiState.pendingVpnPermissionIntent?.let { intent ->
            vpnPermissionLauncher.launch(intent)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = SurfaceElevated,
                    contentColor = TextPrimary, shape = SnackbarShape)
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundGradient()).padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(Dimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
            ) {
                Spacer(Modifier.height(Dimens.SpaceSM))
                ConnectionSection(state = uiState.connectionState, onAction = { viewModel.onMainAction(activity) })
                AnimatedVisibility(visible = uiState.isConnected, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    SessionInfoRow(elapsed = uiState.sessionElapsed, serverIp = uiState.serverIp)
                }
                ActiveProfileCard(profile = uiState.activeProfile, connectionState = uiState.connectionState, onChangeProfile = onNavigateToProfiles)
                QuickActionsRow(isConnected = uiState.isConnected, hasProfiles = uiState.hasProfiles, onViewProfiles = onNavigateToProfiles, onDisconnect = { viewModel.disconnect() })
                Spacer(Modifier.height(Dimens.SpaceLG))
            }
        }
    }
}

@Composable
private fun ConnectionSection(state: VpnConnectionState, onAction: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)) {
        AnimatedContent(targetState = state.label(), transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "status_label") { label ->
            Text(text = label, style = StatusLabelStyle.copy(color = stateColor(state), letterSpacing = 3.sp))
        }
        MainActionButton(state = state, onClick = onAction)
        AnimatedContent(targetState = stateSubtext(state), transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "subtext") { subtext ->
            Text(text = subtext, style = MaterialTheme.typography.bodySmall, color = TextTertiary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MainActionButton(state: VpnConnectionState, onClick: () -> Unit, size: Dp = Dimens.ActionButtonSize) {
    val color = stateColor(state)
    val isConnecting = state is VpnConnectionState.Connecting
    val rotation by rememberInfiniteTransition(label = "rot").animateFloat(0f, 360f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "r")
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(0.2f, 0.55f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "g")
    val scale by animateFloatAsState(if (isConnecting) 0.96f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "s")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size + 32.dp)) {
        Box(Modifier.size(size + 24.dp).clip(CircleShape).background(color.copy(if (state.isConnected) glowAlpha else 0.08f)))
        if (isConnecting) {
            Canvas(Modifier.size(size + 12.dp).rotate(rotation)) {
                drawArc(color, 0f, 260f, false, Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size).scale(scale).clip(CircleShape)
                .background(Brush.radialGradient(listOf(color.copy(0.22f), SurfaceVariant)))
                .border(Dimens.BorderAccent, color, CircleShape)
                .clickable(enabled = state !is VpnConnectionState.Disconnecting, onClick = onClick)
        ) {
            AnimatedContent(state, { scaleIn(tween(200)) + fadeIn(tween(200)) togetherWith scaleOut(tween(150)) + fadeOut(tween(150)) }, label = "icon") { s ->
                Icon(stateIcon(s), s.actionLabel(), tint = color, modifier = Modifier.size(Dimens.ActionButtonIconSize))
            }
        }
        Text(state.actionLabel(), Modifier.align(Alignment.BottomCenter).offset(y = 20.dp),
            style = MaterialTheme.typography.labelLarge.copy(color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
    }
}

@Composable
private fun SessionInfoRow(elapsed: Long, serverIp: String) {
    GhostCard(NeonGreen.copy(0.3f), NeonGreen, NeonGreen.copy(0.05f), PaddingValues(horizontal = Dimens.SpaceXXL, vertical = Dimens.SpaceMD)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            SessionInfoItem(Icons.Filled.Timer, "Tiempo", elapsed.toSessionTime(), NeonGreen)
            VerticalDivider(Modifier.height(36.dp), color = BorderSubtle)
            SessionInfoItem(Icons.Filled.Language, "Servidor", serverIp.ifEmpty { "--" }, NeonCyan, true)
        }
    }
}

@Composable
private fun SessionInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, valueColor: Color, isMono: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = valueColor.copy(0.7f), modifier = Modifier.size(Dimens.IconSM))
        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Text(value, style = if (isMono) MonoStyle.copy(13.sp, color = valueColor) else SessionTimerStyle.copy(18.sp, color = valueColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActiveProfileCard(profile: VpnProfile?, connectionState: VpnConnectionState, onChangeProfile: () -> Unit) {
    GhostCard(if (connectionState.isConnected) NeonCyan.copy(0.4f) else BorderSubtle, if (connectionState.isConnected) NeonCyan else null) {
        Row(Modifier.fillMaxWidth(), Alignment.CenterVertically, Arrangement.SpaceBetween) {
            Box(Modifier.size(Dimens.ProfileIconSize).clip(MaterialTheme.shapes.medium).background(NeonCyan.copy(0.1f)).border(Dimens.BorderThin, NeonCyan.copy(0.3f), MaterialTheme.shapes.medium), Alignment.Center) {
                Icon(Icons.Filled.VpnKey, null, tint = if (profile != null) NeonCyan else TextTertiary, modifier = Modifier.size(Dimens.IconMD))
            }
            Spacer(Modifier.width(Dimens.SpaceMD))
            Column(Modifier.weight(1f)) {
                Text(profile?.name ?: "Sin perfil seleccionado",
                    style = MaterialTheme.typography.titleSmall.copy(color = if (profile != null) TextPrimary else TextTertiary, fontWeight = FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (profile != null) {
                    Text("${profile.host}:${profile.port}  ·  ${profile.method.uppercase()}", style = MonoStyle.copy(11.sp, color = TextTertiary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (profile.tags.isNotEmpty()) {
                        Spacer(Modifier.height(Dimens.SpaceXS))
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) { profile.tags.take(3).forEach { ProfileTagChip(it) } }
                    }
                } else { Text("Toca para seleccionar un perfil", style = MaterialTheme.typography.bodySmall, color = TextTertiary) }
            }
            if (!connectionState.isConnected) { IconButton(onChangeProfile) { Icon(Icons.Filled.SwapHoriz, "Cambiar", tint = NeonCyan) } }
        }
    }
}

@Composable
private fun QuickActionsRow(isConnected: Boolean, hasProfiles: Boolean, onViewProfiles: () -> Unit, onDisconnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        Text("ACCESOS RÁPIDOS", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, letterSpacing = 2.sp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(Dimens.SpaceSM)) {
            QuickActionCard(Icons.Filled.VpnKey, "Perfiles", NeonBlue, Modifier.weight(1f)) { onViewProfiles() }
            QuickActionCard(Icons.Filled.PowerSettingsNew, if (isConnected) "Desconectar" else "Desconectado", if (isConnected) NeonRed else TextTertiary, Modifier.weight(1f), isConnected) { onDisconnect() }
            QuickActionCard(Icons.Filled.Shield, if (isConnected) "Protegido" else "Sin protección", if (isConnected) NeonGreen else NeonAmber, Modifier.weight(1f)) {}
        }
    }
}

@Composable
private fun QuickActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = if (enabled) color else TextTertiary
    GhostCard(modifier.clickable(enabled = enabled, onClick = onClick), c.copy(0.3f), null, c.copy(0.06f), PaddingValues(Dimens.SpaceMD)) {
        Column(Alignment.CenterHorizontally, Arrangement.spacedBy(Dimens.SpaceXS), Modifier.fillMaxWidth()) {
            Icon(icon, label, tint = c, modifier = Modifier.size(Dimens.IconLG))
            Text(label, style = MaterialTheme.typography.labelSmall, color = c, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun stateIcon(s: VpnConnectionState): androidx.compose.ui.graphics.vector.ImageVector = when (s) {
    is VpnConnectionState.Connected -> Icons.Filled.Lock
    is VpnConnectionState.Connecting -> Icons.Filled.Sync
    is VpnConnectionState.Disconnecting -> Icons.Filled.Sync
    is VpnConnectionState.Disconnected -> Icons.Filled.LockOpen
    is VpnConnectionState.Error -> Icons.Filled.ErrorOutline
}

private fun stateSubtext(state: VpnConnectionState): String = when (state) {
    is VpnConnectionState.Disconnected -> "Presiona para conectar"
    is VpnConnectionState.Connecting -> "Estableciendo túnel seguro\u2026"
    is VpnConnectionState.Connected -> "Tu tr\u00e1fico est\u00e1 protegido"
    is VpnConnectionState.Disconnecting -> "Cerrando conexi\u00f3n\u2026"
    is VpnConnectionState.Error -> state.message.ifEmpty { "Fallo en la conexi\u00f3n" }
}
