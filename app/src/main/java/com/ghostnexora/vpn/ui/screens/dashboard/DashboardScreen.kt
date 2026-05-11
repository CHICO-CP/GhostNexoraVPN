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

// ══════════════════════════════════════════════════════════════════════════
// DASHBOARD SCREEN
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun DashboardScreen(
    onNavigateToProfiles: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher para el diálogo de permiso VPN del sistema
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onVpnPermissionGranted()
        else viewModel.onVpnPermissionDenied()
    }

    // Lanzar diálogo VPN cuando el ViewModel lo solicite
    LaunchedEffect(uiState.pendingVpnPermissionIntent) {
        uiState.pendingVpnPermissionIntent?.let {
            vpnPermissionLauncher.launch(it)
        }
    }

    // Snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient())
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
            ) {
                Spacer(Modifier.height(Dimens.SpaceSM))

                // ── 1. Estado de conexión + botón principal ─────────────────
                ConnectionSection(
                    state = uiState.connectionState,
                    onAction = { viewModel.onMainAction(activity) }
                )

                // ── 2. Timer de sesión ──────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.isConnected,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    SessionInfoRow(
                        elapsed  = uiState.sessionElapsed,
                        serverIp = uiState.serverIp
                    )
                }

                // ── 3. Perfil activo ────────────────────────────────────────
                ActiveProfileCard(
                    profile             = uiState.activeProfile,
                    connectionState     = uiState.connectionState,
                    onChangeProfile     = onNavigateToProfiles
                )

                // ── 4. Accesos rápidos ──────────────────────────────────────
                QuickActionsRow(
                    isConnected         = uiState.isConnected,
                    hasProfiles         = uiState.hasProfiles,
                    onViewProfiles      = onNavigateToProfiles,
                    onDisconnect        = { viewModel.disconnect() }
                )

                Spacer(Modifier.height(Dimens.SpaceLG))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// SECCIÓN DE CONEXIÓN — Botón principal + estado
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun ConnectionSection(
    state: VpnConnectionState,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
    ) {
        // Etiqueta de estado animada
        AnimatedContent(
            targetState = state.label(),
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
            },
            label = "status_label"
        ) { label ->
            Text(
                text = label,
                style = StatusLabelStyle.copy(
                    color = stateColor(state),
                    letterSpacing = 3.sp
                )
            )
        }

        // Botón circular principal
        MainActionButton(
            state    = state,
            onClick  = onAction,
            size     = Dimens.ActionButtonSize
        )

        // Subtexto contextual
        AnimatedContent(
            targetState = stateSubtext(state),
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "status_subtext"
        ) { subtext ->
            Text(
                text  = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// BOTÓN PRINCIPAL CIRCULAR CON ANIMACIONES
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun MainActionButton(
    state: VpnConnectionState,
    onClick: () -> Unit,
    size: Dp = 120.dp
) {
    val color = stateColor(state)
    val isConnecting = state is VpnConnectionState.Connecting

    // Rotación del anillo cuando conecta
    val rotation by rememberInfiniteTransition(label = "ring_rotation")
        .animateFloat(
            initialValue = 0f,
            targetValue  = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing)
            ),
            label = "ring_angle"
        )

    // Pulso del glow exterior
    val glowAlpha by rememberInfiniteTransition(label = "glow_pulse")
        .animateFloat(
            initialValue = 0.2f,
            targetValue  = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )

    // Escala del botón al presionar
    val scale by animateFloatAsState(
        targetValue = if (state is VpnConnectionState.Connecting) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 32.dp)         // espacio para el glow exterior
    ) {
        // ── Glow exterior ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(size + 24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (state.isConnected) glowAlpha else 0.08f))
        )

        // ── Anillo giratorio (solo al conectar) ───────────────────────────
        if (isConnecting) {
            Canvas(
                modifier = Modifier
                    .size(size + 12.dp)
                    .rotate(rotation)
            ) {
                drawArc(
                    color      = color,
                    startAngle = 0f,
                    sweepAngle = 260f,
                    useCenter  = false,
                    style      = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // ── Círculo principal ─────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.22f),
                            SurfaceVariant
                        )
                    )
                )
                .border(
                    width = Dimens.BorderAccent,
                    color = color,
                    shape = CircleShape
                )
                .clickable(
                    enabled = state !is VpnConnectionState.Disconnecting,
                    onClick = onClick
                )
        ) {
            // Icono animado según estado
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    scaleIn(tween(200)) + fadeIn(tween(200)) togetherWith
                    scaleOut(tween(150)) + fadeOut(tween(150))
                },
                label = "btn_icon"
            ) { s ->
                Icon(
                    imageVector = stateIcon(s),
                    contentDescription = s.actionLabel(),
                    tint = color,
                    modifier = Modifier.size(Dimens.ActionButtonIconSize)
                )
            }
        }

        // ── Texto del botón debajo ─────────────────────────────────────────
        Text(
            text  = state.actionLabel(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 20.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// FILA DE INFO DE SESIÓN (timer + IP)
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionInfoRow(
    elapsed: Long,
    serverIp: String
) {
    GhostCard(
        borderColor = NeonGreen.copy(alpha = 0.3f),
        glowColor   = NeonGreen,
        backgroundColor = NeonGreen.copy(alpha = 0.05f),
        contentPadding = PaddingValues(horizontal = Dimens.SpaceXXL, vertical = Dimens.SpaceMD)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer
            SessionInfoItem(
                icon  = Icons.Filled.Timer,
                label = "Tiempo",
                value = elapsed.toSessionTime(),
                valueColor = NeonGreen
            )

            VerticalDivider(
                modifier = Modifier.height(36.dp),
                color = BorderSubtle
            )

            // IP del servidor
            SessionInfoItem(
                icon  = Icons.Filled.Language,
                label = "Servidor",
                value = serverIp.ifEmpty { "--" },
                valueColor = NeonCyan,
                isMono = true
            )
        }
    }
}

@Composable
private fun SessionInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    isMono: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = valueColor.copy(alpha = 0.7f),
            modifier = Modifier.size(Dimens.IconSM)
        )
        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Text(
            text  = value,
            style = if (isMono) MonoStyle.copy(fontSize = 13.sp, color = valueColor)
                    else SessionTimerStyle.copy(fontSize = 18.sp, color = valueColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// TARJETA DE PERFIL ACTIVO
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun ActiveProfileCard(
    profile: VpnProfile?,
    connectionState: VpnConnectionState,
    onChangeProfile: () -> Unit
) {
    GhostCard(
        borderColor = if (connectionState.isConnected) NeonCyan.copy(0.4f) else BorderSubtle,
        glowColor   = if (connectionState.isConnected) NeonCyan else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Ícono del perfil
            Box(
                modifier = Modifier
                    .size(Dimens.ProfileIconSize)
                    .clip(MaterialTheme.shapes.medium)
                    .background(NeonCyan.copy(alpha = 0.1f))
                    .border(Dimens.BorderThin, NeonCyan.copy(0.3f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = null,
                    tint = if (profile != null) NeonCyan else TextTertiary,
                    modifier = Modifier.size(Dimens.IconMD)
                )
            }

            Spacer(Modifier.width(Dimens.SpaceMD))

            // Info del perfil
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.name ?: "Sin perfil seleccionado",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (profile != null) TextPrimary else TextTertiary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (profile != null) {
                    Text(
                        text  = "${profile.host}:${profile.port}  ·  ${profile.method.uppercase()}",
                        style = MonoStyle.copy(fontSize = 11.sp, color = TextTertiary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Tags
                    if (profile.tags.isNotEmpty()) {
                        Spacer(Modifier.height(Dimens.SpaceXS))
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
                            profile.tags.take(3).forEach { tag ->
                                ProfileTagChip(tag)
                            }
                        }
                    }
                } else {
                    Text(
                        text  = "Toca para seleccionar un perfil",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Botón cambiar perfil (solo si no está conectado)
            if (!connectionState.isConnected) {
                IconButton(onClick = onChangeProfile) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "Cambiar perfil",
                        tint = NeonCyan
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// ACCESOS RÁPIDOS
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(
    isConnected: Boolean,
    hasProfiles: Boolean,
    onViewProfiles: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        Text(
            text  = "ACCESOS RÁPIDOS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                letterSpacing = 2.sp
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
        ) {
            // Ver perfiles
            QuickActionCard(
                icon  = Icons.Filled.VpnKey,
                label = "Perfiles",
                color = NeonBlue,
                modifier = Modifier.weight(1f),
                onClick = onViewProfiles
            )

            // Desconectar rápido (solo si conectado)
            QuickActionCard(
                icon    = Icons.Filled.PowerSettingsNew,
                label   = if (isConnected) "Desconectar" else "Desconectado",
                color   = if (isConnected) NeonRed else TextTertiary,
                enabled = isConnected,
                modifier = Modifier.weight(1f),
                onClick = onDisconnect
            )

            // Estado del sistema
            QuickActionCard(
                icon  = Icons.Filled.Shield,
                label = if (isConnected) "Protegido" else "Sin protección",
                color = if (isConnected) NeonGreen else NeonAmber,
                modifier = Modifier.weight(1f),
                onClick = {}
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val resolvedColor = if (enabled) color else TextTertiary

    GhostCard(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
        borderColor = resolvedColor.copy(alpha = 0.3f),
        backgroundColor = resolvedColor.copy(alpha = 0.06f),
        contentPadding = PaddingValues(Dimens.SpaceMD)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = resolvedColor,
                modifier = Modifier.size(Dimens.IconLG)
            )
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = resolvedColor,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// HELPERS LOCALES
// ══════════════════════════════════════════════════════════════════════════

private fun stateIcon(
    state: VpnConnectionState
): androidx.compose.ui.graphics.vector.ImageVector = when (state) {
    is VpnConnectionState.Connected     -> Icons.Filled.Lock
    is VpnConnectionState.Connecting    -> Icons.Filled.Sync
    is VpnConnectionState.Disconnecting -> Icons.Filled.Sync
    is VpnConnectionState.Disconnected  -> Icons.Filled.LockOpen
    is VpnConnectionState.Error         -> Icons.Filled.ErrorOutline
}

private fun stateSubtext(state: VpnConnectionState): String = when (state) {
    is VpnConnectionState.Disconnected  -> "Presiona para conectar"
    is VpnConnectionState.Connecting    -> "Estableciendo túnel seguro…"
    is VpnConnectionState.Connected     -> "Tu tráfico está protegido"
    is VpnConnectionState.Disconnecting -> "Cerrando conexión…"
    is VpnConnectionState.Error         -> state.message.ifEmpty { "Fallo en la conexión" }
}
