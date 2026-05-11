package com.ghostnexora.vpn.data.model

import androidx.room.ColumnInfo

/**
 * Configuración de proxy embebida en VpnProfile.
 * Se almacena como tipo embebido en Room.
 */
data class ProxyConfig(
    @ColumnInfo(name = "proxy_host")
    val host: String = "",

    @ColumnInfo(name = "proxy_port")
    val port: Int = 0,

    @ColumnInfo(name = "proxy_type")
    val type: String = "" // "http", "socks5", ""
)
