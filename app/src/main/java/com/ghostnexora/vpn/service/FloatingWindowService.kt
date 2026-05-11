package com.ghostnexora.vpn.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ghostnexora.vpn.GhostNexoraApp
import com.ghostnexora.vpn.R
import com.ghostnexora.vpn.data.model.VpnConnectionState
import com.ghostnexora.vpn.data.repository.ProfileRepository
import com.ghostnexora.vpn.ui.MainActivity
import com.ghostnexora.vpn.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * FloatingWindowService — Ventana flotante de control rápido VPN.
 *
 * Muestra una burbuja sobre otras apps con:
 * - Indicador de estado (color dinámico)
 * - Panel expandible con botones de acción
 * - Draggable (arrastrable)
 *
 * Requiere permiso SYSTEM_ALERT_WINDOW.
 */
@AndroidEntryPoint
class FloatingWindowService : android.app.Service(),
    LifecycleOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var repository: ProfileRepository

    // ── WindowManager ─────────────────────────────────────────────────────
    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    // ── Scope ─────────────────────────────────────────────────────────────
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── Estado reactivo ───────────────────────────────────────────────────
    private val _vpnState = mutableStateOf<VpnConnectionState>(VpnConnectionState.Disconnected)
    private val _profileName = mutableStateOf("")
    private val _panelExpanded = mutableStateOf(false)

    // ── Lifecycle para Compose en Service ─────────────────────────────────
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ══════════════════════════════════════════════════════════════════════
    // CICLO DE VIDA
    // ══════════════════════════════════════════════════════════════════════

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(GhostNexoraApp.NOTIF_ID_FLOATING, buildNotification())
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        showBubble()
        observeVpnState()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeBubble()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ══════════════════════════════════════════════════════════════════════
    // BURBUJA FLOTANTE
    // ══════════════════════════════════════════════════════════════════════

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 200
        }
        bubbleParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)

            setContent {
                val vpnState by _vpnState
                val profileName by _profileName
                val expanded by _panelExpanded

                FloatingBubbleContent(
                    vpnState    = vpnState,
                    profileName = profileName,
                    expanded    = expanded,
                    onToggle    = { _panelExpanded.value = !_panelExpanded.value },
                    onConnect   = { sendConnectAction() },
                    onDisconnect = { sendDisconnectAction() },
                    onOpen      = { openMainApp() },
                    onClose     = { stopSelf() }
                )
            }
        }

        // Touch para arrastrar la burbuja
        var initialX = 0; var initialY = 0
        var touchX = 0f; var touchY = 0f

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(composeView, params)
                    true
                }
                else -> false
            }
        }

        bubbleView = composeView
        windowManager.addView(composeView, params)
    }

    private fun removeBubble() {
        bubbleView?.let {
            runCatching { windowManager.removeView(it) }
            bubbleView = null
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // OBSERVAR ESTADO VPN
    // ══════════════════════════════════════════════════════════════════════

    private fun observeVpnState() {
        serviceScope.launch {
            GhostVpnService.connectionState.collectLatest { state ->
                _vpnState.value = state
                _profileName.value = when (state) {
                    is VpnConnectionState.Connected  -> state.profileName
                    is VpnConnectionState.Connecting -> state.profileName
                    else -> ""
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACCIONES
    // ══════════════════════════════════════════════════════════════════════

    private fun sendConnectAction() {
        serviceScope.launch {
            val profileId = repository.activeProfileId
                .kotlinx.coroutines.flow.first()
            if (profileId.isNotEmpty()) {
                startService(Intent(this@FloatingWindowService, GhostVpnService::class.java).apply {
                    action = GhostVpnService.ACTION_CONNECT
                    putExtra(GhostVpnService.EXTRA_PROFILE_ID, profileId)
                })
            }
        }
    }

    private fun sendDisconnectAction() {
        startService(Intent(this, GhostVpnService::class.java).apply {
            action = GhostVpnService.ACTION_DISCONNECT
        })
    }

    private fun openMainApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }

    // ══════════════════════════════════════════════════════════════════════
    // NOTIFICACIÓN DEL FOREGROUND
    // ══════════════════════════════════════════════════════════════════════

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, GhostNexoraApp.CHANNEL_FLOATING_WINDOW)
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setContentTitle("Control flotante activo")
            .setContentText("Toca para abrir Ghost Nexora VPN")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}

// ══════════════════════════════════════════════════════════════════════════
// COMPOSABLE — CONTENIDO DE LA BURBUJA
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun FloatingBubbleContent(
    vpnState: VpnConnectionState,
    profileName: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit
) {
    val stateColor = stateColor(vpnState)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Panel expandido ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn() + slideInHorizontally { it },
            exit    = fadeOut() + slideOutHorizontally { it }
        ) {
            FloatingPanel(
                vpnState    = vpnState,
                profileName = profileName,
                stateColor  = stateColor,
                onConnect   = onConnect,
                onDisconnect = onDisconnect,
                onOpen      = onOpen
            )
        }

        // ── Burbuja principal ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(Dimens.BubbleSize)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(2.dp, stateColor, CircleShape)
                .neonGlow(stateColor, radius = 8.dp, alpha = 0.4f)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (vpnState) {
                        is VpnConnectionState.Connected  -> Icons.Filled.Lock
                        is VpnConnectionState.Connecting -> Icons.Filled.Sync
                        is VpnConnectionState.Error      -> Icons.Filled.ErrorOutline
                        else                             -> Icons.Filled.LockOpen
                    },
                    contentDescription = null,
                    tint     = stateColor,
                    modifier = Modifier.size(Dimens.BubbleIconSize)
                )
            }
        }

        // ── Botón cerrar burbuja ───────────────────────────────────────────
        if (expanded) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NeonRed.copy(0.15f))
                    .border(1.dp, NeonRed.copy(0.5f), CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = NeonRed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingPanel(
    vpnState: VpnConnectionState,
    profileName: String,
    stateColor: Color,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(Dimens.FloatingPanelWidth)
            .clip(BubbleShape)
            .background(SurfaceDark)
            .border(1.dp, stateColor.copy(0.3f), BubbleShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Estado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
            Text(
                text  = vpnState.label(),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = stateColor
                )
            )
        }

        // Nombre del perfil
        if (profileName.isNotEmpty()) {
            Text(
                text  = profileName,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp,
                    color    = TextSecondary
                ),
                maxLines = 1
            )
        }

        NeonDivider(color = BorderSubtle)

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Conectar / Desconectar
            if (vpnState.isConnected) {
                FloatingActionBtn(
                    icon    = Icons.Filled.PowerSettingsNew,
                    label   = "Desconectar",
                    color   = NeonRed,
                    onClick = onDisconnect
                )
            } else if (vpnState.isDisconnected || vpnState.hasError) {
                FloatingActionBtn(
                    icon    = Icons.Filled.PlayArrow,
                    label   = "Conectar",
                    color   = NeonGreen,
                    onClick = onConnect
                )
            }

            // Abrir app
            FloatingActionBtn(
                icon    = Icons.Filled.OpenInFull,
                label   = "Abrir",
                color   = NeonCyan,
                onClick = onOpen
            )
        }
    }
}

@Composable
private fun FloatingActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(0.12f))
                .border(1.dp, color.copy(0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text  = label,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 9.sp,
                color    = color
            )
        )
    }
}
