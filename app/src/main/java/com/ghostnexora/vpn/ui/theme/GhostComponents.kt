package com.ghostnexora.vpn.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghostnexora.vpn.data.model.VpnConnectionState
import com.ghostnexora.vpn.data.model.LogLevel

// ══════════════════════════════════════════════════════════════════════════
// GHOST NEXORA VPN — COMPONENTES REUTILIZABLES
// ══════════════════════════════════════════════════════════════════════════

// ── 1. Tarjeta base con borde neon ────────────────────────────────────────

/**
 * Contenedor estilo tarjeta con fondo oscuro y borde sutil.
 * @param glowColor Si no es null, añade un sutil efecto glow al borde.
 */
@Composable
fun GhostCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderSubtle,
    glowColor: Color? = null,
    backgroundColor: Color = SurfaceVariant,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val borderMod = if (glowColor != null) {
        modifier.neonGlow(glowColor, radius = 12.dp, alpha = 0.3f)
    } else modifier

    Column(
        modifier = borderMod
            .clip(ProfileCardShape)
            .background(backgroundColor)
            .border(Dimens.BorderNormal, borderColor, ProfileCardShape)
            .padding(contentPadding)
    ) {
        content()
    }
}

// ── 2. Indicador de estado pulsante ──────────────────────────────────────

/**
 * Punto de estado animado que pulsa cuando está conectando.
 */
@Composable
fun StatusDot(
    state: VpnConnectionState,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.StatusDotSize
) {
    val color = stateColor(state)
    val isPulsing = state is VpnConnectionState.Connecting

    val alpha by if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ── 3. Badge de nivel de log ──────────────────────────────────────────────

@Composable
fun LogLevelBadge(level: LogLevel, modifier: Modifier = Modifier) {
    val (bg, text) = when (level) {
        LogLevel.DEBUG   -> SurfaceElevated to TextTertiary
        LogLevel.INFO    -> NeonBlue.copy(alpha = 0.15f) to NeonBlue
        LogLevel.SUCCESS -> NeonGreen.copy(alpha = 0.15f) to NeonGreen
        LogLevel.WARNING -> NeonAmber.copy(alpha = 0.15f) to NeonAmber
        LogLevel.ERROR   -> NeonRed.copy(alpha = 0.15f) to NeonRed
    }
    Box(
        modifier = modifier
            .clip(TagShape)
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelSmall,
            color = text,
            maxLines = 1
        )
    }
}

// ── 4. Chip de tag de perfil ──────────────────────────────────────────────

@Composable
fun ProfileTagChip(tag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(TagShape)
            .background(NeonCyan.copy(alpha = 0.1f))
            .border(Dimens.BorderThin, NeonCyanDim.copy(alpha = 0.5f), TagShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyanDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── 5. Separador con gradiente ────────────────────────────────────────────

@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    color: Color = BorderSubtle
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = Dimens.BorderThin,
        color = color
    )
}

// ── 6. Botón neon principal ───────────────────────────────────────────────

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = NeonCyan,
    contentColor: Color = TextOnAccent
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SurfaceElevated,
            disabledContentColor = TextDisabled
        ),
        shape = InputFieldShape
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ── 7. Botón secundario (outline) ─────────────────────────────────────────

@Composable
fun GhostOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = NeonCyan,
    contentColor: Color = NeonCyan
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = TextDisabled
        ),
        border = androidx.compose.foundation.BorderStroke(
            Dimens.BorderNormal,
            if (enabled) borderColor else BorderNormal
        ),
        shape = InputFieldShape
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ── 8. Campo de texto estilizado ──────────────────────────────────────────

@Composable
fun GhostTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = TextTertiary) }
            } else null,
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
            shape = InputFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = BorderNormal,
                errorBorderColor = NeonRed,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = TextSecondary,
                cursorColor = NeonCyan,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextDisabled,
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
                disabledContainerColor = SurfaceDark
            )
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = NeonRed,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// HELPERS Y EXTENSIONES
// ══════════════════════════════════════════════════════════════════════════

/** Devuelve el color correcto para cada estado de conexión */
fun stateColor(state: VpnConnectionState): Color = when (state) {
    is VpnConnectionState.Connected     -> StateConnected
    is VpnConnectionState.Connecting    -> StateConnecting
    is VpnConnectionState.Disconnecting -> StateConnecting
    is VpnConnectionState.Disconnected  -> StateDisconnected
    is VpnConnectionState.Error         -> StateError
}

/** Efecto de glow neon usando drawBehind */
fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 16.dp,
    alpha: Float = 0.4f
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    radius.toPx(),
                    0f, 0f,
                    color.copy(alpha = alpha).toArgb()
                )
            }
        }
        canvas.drawRoundRect(
            left = 0f, top = 0f,
            right = size.width, bottom = size.height,
            radiusX = 16.dp.toPx(), radiusY = 16.dp.toPx(),
            paint = paint
        )
    }
}

/** Gradiente vertical oscuro para fondos de sección */
fun backgroundGradient() = Brush.verticalGradient(
    colors = listOf(BackgroundDeep, BackgroundDark)
)

/** Gradiente radial para el botón de acción del Dashboard */
fun actionButtonGradient(color: Color) = Brush.radialGradient(
    colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
)
