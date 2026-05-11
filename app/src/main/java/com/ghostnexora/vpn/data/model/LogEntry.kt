package com.ghostnexora.vpn.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro de actividad del servicio VPN.
 * Cada entrada representa un evento en la sesión.
 */
@Entity(tableName = "log_entries")
data class LogEntry(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "level")
    val level: LogLevel = LogLevel.INFO,

    @ColumnInfo(name = "tag")
    val tag: String = "VPN",

    @ColumnInfo(name = "message")
    val message: String = "",

    @ColumnInfo(name = "profile_id")
    val profileId: String? = null
) {
    /** Formatea el timestamp como HH:mm:ss */
    val timeFormatted: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }

    /** Formatea fecha completa para logs detallados */
    val dateTimeFormatted: String
        get() {
            val sdf = java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault()
            )
            return sdf.format(java.util.Date(timestamp))
        }
}

/**
 * Niveles de log ordenados por severidad.
 */
enum class LogLevel(val label: String, val emoji: String) {
    DEBUG("DEBUG", "🔍"),
    INFO("INFO", "ℹ️"),
    SUCCESS("SUCCESS", "✅"),
    WARNING("WARNING", "⚠️"),
    ERROR("ERROR", "❌")
}
