package com.ghostnexora.vpn.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ══════════════════════════════════════════════════════════════════════════
// GHOST NEXORA VPN — FORMAS
// Bordes suaves y modernos, consistentes con Material Design 3
// ══════════════════════════════════════════════════════════════════════════

val GhostShapes = Shapes(
    // Chips, badges, elementos pequeños
    extraSmall = RoundedCornerShape(4.dp),

    // Botones, campos de texto
    small = RoundedCornerShape(8.dp),

    // Tarjetas de perfil, dialogs pequeños
    medium = RoundedCornerShape(12.dp),

    // Tarjetas principales, drawers
    large = RoundedCornerShape(16.dp),

    // Bottom sheets, modals grandes
    extraLarge = RoundedCornerShape(24.dp)
)

// ── Formas custom adicionales ─────────────────────────────────────────────

/** Botón principal circular del Dashboard */
val CircularShape = RoundedCornerShape(50)

/** Tarjeta de perfil activo */
val ProfileCardShape = RoundedCornerShape(16.dp)

/** Burbuja de estado flotante */
val BubbleShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 4.dp,
    bottomEnd = 20.dp
)

/** Snackbar personalizado */
val SnackbarShape = RoundedCornerShape(12.dp)

/** Tag/chip de perfil */
val TagShape = RoundedCornerShape(50)

/** Campo de texto en formularios */
val InputFieldShape = RoundedCornerShape(10.dp)
