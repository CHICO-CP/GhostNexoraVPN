package com.ghostnexora.vpn.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════
// GHOST NEXORA VPN — PALETA DE COLORES
// Tema oscuro con acentos neon cian/azul/verde (Material Design 3)
// ══════════════════════════════════════════════════════════════════════════

// ── Fondos ────────────────────────────────────────────────────────────────
val BackgroundDeep    = Color(0xFF0A0E1A)   // Fondo más oscuro (raíz)
val BackgroundDark    = Color(0xFF0D1117)   // Fondo principal
val SurfaceDark       = Color(0xFF111827)   // Superficie base
val SurfaceVariant    = Color(0xFF1A2235)   // Tarjetas y contenedores
val SurfaceElevated   = Color(0xFF1E2D42)   // Tarjetas elevadas
val SurfaceDim        = Color(0xFF0F1923)   // Overlay sutil

// ── Acentos Neon ──────────────────────────────────────────────────────────
val NeonCyan          = Color(0xFF00E5FF)   // Acento principal
val NeonCyanDim       = Color(0xFF00B8D4)   // Cyan apagado
val NeonCyanGlow      = Color(0x4000E5FF)   // Cyan con transparencia (glow)
val NeonBlue          = Color(0xFF4FC3F7)   // Azul claro
val NeonBlueDark      = Color(0xFF0288D1)   // Azul oscuro
val NeonGreen         = Color(0xFF00E676)   // Verde conexión activa
val NeonGreenDim      = Color(0xFF00C853)   // Verde apagado
val NeonGreenGlow     = Color(0x4000E676)   // Verde glow
val NeonAmber         = Color(0xFFFFD740)   // Advertencia
val NeonRed           = Color(0xFFFF5252)   // Error / desconexión
val NeonRedDim        = Color(0xFFD50000)   // Rojo oscuro
val NeonRedGlow       = Color(0x40FF5252)   // Rojo glow
val NeonPurple        = Color(0xFFCE93D8)   // Acento secundario

// ── Texto ─────────────────────────────────────────────────────────────────
val TextPrimary       = Color(0xFFFFFFFF)   // Texto principal
val TextSecondary     = Color(0xFF9CA3AF)   // Texto secundario
val TextTertiary      = Color(0xFF6B7280)   // Texto terciario / placeholder
val TextDisabled      = Color(0xFF374151)   // Texto deshabilitado
val TextOnAccent      = Color(0xFF000000)   // Texto sobre botones neon

// ── Bordes y Divisores ────────────────────────────────────────────────────
val BorderSubtle      = Color(0xFF1F2937)   // Borde muy sutil
val BorderNormal      = Color(0xFF374151)   // Borde estándar
val BorderAccent      = Color(0xFF00E5FF)   // Borde resaltado (neon)

// ── Estados de Conexión ───────────────────────────────────────────────────
val StateConnected    = NeonGreen
val StateConnecting   = NeonAmber
val StateDisconnected = TextSecondary
val StateError        = NeonRed

// ── Material 3 — Color Scheme Tokens (Dark) ───────────────────────────────
// Primary
val Md3Primary        = NeonCyan
val Md3OnPrimary      = Color(0xFF003544)
val Md3PrimaryContainer    = Color(0xFF004E63)
val Md3OnPrimaryContainer  = NeonCyan

// Secondary
val Md3Secondary      = NeonBlue
val Md3OnSecondary    = Color(0xFF003547)
val Md3SecondaryContainer  = Color(0xFF004D64)
val Md3OnSecondaryContainer = NeonBlue

// Tertiary
val Md3Tertiary       = NeonPurple
val Md3OnTertiary     = Color(0xFF4A148C)
val Md3TertiaryContainer   = Color(0xFF6A1B9A)
val Md3OnTertiaryContainer = NeonPurple

// Error
val Md3Error          = NeonRed
val Md3OnError        = Color(0xFF690005)
val Md3ErrorContainer      = Color(0xFF93000A)
val Md3OnErrorContainer    = Color(0xFFFFDAD6)

// Background / Surface
val Md3Background     = BackgroundDark
val Md3OnBackground   = TextPrimary
val Md3Surface        = SurfaceDark
val Md3OnSurface      = TextPrimary
val Md3SurfaceVariant = SurfaceVariant
val Md3OnSurfaceVariant    = TextSecondary
val Md3Outline        = BorderNormal
val Md3OutlineVariant = BorderSubtle
val Md3InverseSurface      = Color(0xFFE2E8F0)
val Md3InverseOnSurface    = Color(0xFF1A2235)
val Md3InversePrimary      = Color(0xFF006782)
val Md3SurfaceTint    = NeonCyan
val Md3Scrim          = Color(0xFF000000)
