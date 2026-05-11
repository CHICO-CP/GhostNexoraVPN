package com.ghostnexora.vpn.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ══════════════════════════════════════════════════════════════════════════
// GHOST NEXORA VPN — EXTENSIONES KOTLIN
// Funciones de utilidad reutilizables en toda la app
// ══════════════════════════════════════════════════════════════════════════

// ── Long (timestamps / duración) ──────────────────────────────────────────

/**
 * Convierte milisegundos de duración en formato HH:mm:ss
 * Ejemplo: 3661000L → "01:01:01"
 */
fun Long.toSessionTime(): String {
    val hours   = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

/**
 * Convierte un timestamp epoch a fecha legible.
 * Ejemplo: 1715000000000L → "06/05/2026 14:30"
 */
fun Long.toReadableDate(
    pattern: String = "dd/MM/yyyy HH:mm"
): String {
    if (this == 0L) return "--"
    return SimpleDateFormat(pattern, Locale.getDefault())
        .format(Date(this))
}

/**
 * Convierte un timestamp epoch a tiempo relativo.
 * Ejemplo: "hace 3 minutos", "hace 2 horas"
 */
fun Long.toRelativeTime(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < TimeUnit.MINUTES.toMillis(1)  -> "hace un momento"
        diff < TimeUnit.HOURS.toMillis(1)    -> "hace ${TimeUnit.MILLISECONDS.toMinutes(diff)} min"
        diff < TimeUnit.DAYS.toMillis(1)     -> "hace ${TimeUnit.MILLISECONDS.toHours(diff)} h"
        else                                 -> "hace ${TimeUnit.MILLISECONDS.toDays(diff)} días"
    }
}

// ── String ────────────────────────────────────────────────────────────────

/** Valida que la cadena es un host válido (dominio o IP) */
fun String.isValidHost(): Boolean {
    if (isBlank()) return false
    val ipv4 = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
    val domain = Regex("""^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z]{2,})+$""")
    return ipv4.matches(this) || domain.matches(this)
}

/** Valida que el puerto está en rango válido (1–65535) */
fun String.isValidPort(): Boolean {
    val port = toIntOrNull() ?: return false
    return port in 1..65535
}

/** Trunca el texto y añade "…" si supera [maxLength] */
fun String.truncate(maxLength: Int = 30): String =
    if (length > maxLength) "${take(maxLength)}…" else this

/** Convierte string de tags separados por coma a lista limpia */
fun String.toTagList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }

/** Oculta contraseña mostrando solo los últimos 3 chars */
fun String.maskPassword(): String =
    if (length <= 3) "•".repeat(length) else "•".repeat(length - 3) + takeLast(3)

// ── Int ───────────────────────────────────────────────────────────────────

/** Valida que el entero es un puerto válido */
fun Int.isValidPort(): Boolean = this in 1..65535

// ── Context ───────────────────────────────────────────────────────────────

/** Muestra un Toast corto */
fun Context.showToast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

/** Muestra un Toast largo */
fun Context.showToastLong(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

/**
 * Comparte un archivo mediante el sistema de Android.
 * Usa FileProvider para compatibilidad con Android 7+.
 */
fun Context.shareFile(file: File, mimeType: String = "application/json") {
    val uri: Uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Compartir perfil VPN"))
}

/**
 * Abre la configuración de la app en Ajustes del sistema.
 * Útil para redirigir al usuario a conceder permisos manualmente.
 */
fun Context.openAppSettings() {
    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

// ── Flow ──────────────────────────────────────────────────────────────────

/**
 * Captura errores en un Flow y emite un valor por defecto en su lugar.
 * Evita que un error en DataStore o Room rompa toda la UI.
 */
fun <T> Flow<T>.catchWithDefault(default: T): Flow<T> =
    catch { emit(default) }

/**
 * Mapea un Flow<T?> filtrando los valores nulos.
 */
fun <T, R> Flow<T?>.mapNotNull(transform: (T) -> R): Flow<R?> =
    map { it?.let(transform) }

// ── File ──────────────────────────────────────────────────────────────────

/** Devuelve el tamaño del archivo en formato legible (KB, MB) */
fun File.readableSize(): String {
    val kb = length() / 1024.0
    return if (kb < 1024) "%.1f KB".format(kb)
    else "%.1f MB".format(kb / 1024.0)
}

/** Crea el directorio padre si no existe */
fun File.ensureParentExists(): File {
    parentFile?.mkdirs()
    return this
}
