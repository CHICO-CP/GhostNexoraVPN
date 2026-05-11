package com.ghostnexora.vpn.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ghostnexora.vpn.GhostNexoraApp
import com.ghostnexora.vpn.R
import com.ghostnexora.vpn.data.model.VpnConnectionState
import com.ghostnexora.vpn.ui.MainActivity

/**
 * Helper centralizado para construir y actualizar notificaciones VPN.
 * Separa la lógica de notificaciones del servicio principal.
 */
object VpnNotificationHelper {

    /**
     * Construye la notificación completa según el estado de conexión.
     */
    fun build(context: Context, state: VpnConnectionState): Notification {
        val openIntent  = openAppIntent(context)
        val disconnectIntent = disconnectIntent(context)

        val (title, body) = notificationText(state)

        val builder = NotificationCompat.Builder(
            context,
            GhostNexoraApp.CHANNEL_VPN_STATUS
        )
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setShowWhen(state is VpnConnectionState.Connected)

        // Acción rápida de desconexión
        if (state is VpnConnectionState.Connected) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_vpn_notification,
                    "Desconectar",
                    disconnectIntent
                ).build()
            )
        }

        // Progreso indeterminado al conectar
        if (state is VpnConnectionState.Connecting ||
            state is VpnConnectionState.Disconnecting) {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    /**
     * Actualiza la notificación sin recrearla.
     */
    fun update(context: Context, state: VpnConnectionState) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.notify(GhostNexoraApp.NOTIF_ID_VPN, build(context, state))
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private fun notificationText(state: VpnConnectionState): Pair<String, String> =
        when (state) {
            is VpnConnectionState.Connected ->
                "🔒 VPN Conectada" to
                "${state.profileName}  ·  ${state.serverIp}"

            is VpnConnectionState.Connecting ->
                "⏳ Conectando VPN…" to
                "Perfil: ${state.profileName}"

            is VpnConnectionState.Disconnecting ->
                "⏳ Desconectando…" to
                "Cerrando la conexión VPN"

            is VpnConnectionState.Error ->
                "❌ Error de VPN" to
                state.message.ifEmpty { "Error al conectar" }

            is VpnConnectionState.Disconnected ->
                "Ghost Nexora VPN" to
                "Sin conexión activa"
        }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun disconnectIntent(context: Context): PendingIntent =
        PendingIntent.getService(
            context, 1,
            Intent(context, GhostVpnService::class.java).apply {
                action = GhostVpnService.ACTION_DISCONNECT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
