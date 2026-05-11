package com.ghostnexora.vpn.data.local

import androidx.room.TypeConverter
import com.ghostnexora.vpn.data.model.LogLevel

/**
 * Conversores de tipos para Room.
 * Room solo maneja tipos primitivos nativamente;
 * cualquier tipo personalizado necesita un conversor.
 */
class Converters {

    // ── LogLevel ──────────────────────────────────────────────────────────

    @TypeConverter
    fun fromLogLevel(level: LogLevel): String = level.name

    @TypeConverter
    fun toLogLevel(value: String): LogLevel =
        runCatching { LogLevel.valueOf(value) }.getOrDefault(LogLevel.INFO)

    // ── Long? (nullable timestamp) ────────────────────────────────────────

    @TypeConverter
    fun fromNullableLong(value: Long?): String = value?.toString() ?: ""

    @TypeConverter
    fun toNullableLong(value: String): Long? = value.toLongOrNull()
}
