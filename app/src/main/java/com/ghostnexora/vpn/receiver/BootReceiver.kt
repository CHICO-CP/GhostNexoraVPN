package com.ghostnexora.vpn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghostnexora.vpn.data.local.DataStoreManager
import com.ghostnexora.vpn.service.GhostVpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receptor de arranque del sistema.
 * Si el usuario habilitó "Reconectar al inicio", relanza el VPN Service
 * automáticamente después de que el dispositivo arranca.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dataStore: DataStoreManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        // Usar goAsync para operaciones suspendidas en BroadcastReceiver
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val shouldReconnect = dataStore.reconnectOnBoot.first()
                val activeProfileId = dataStore.activeProfileId.first()

                if (shouldReconnect && activeProfileId.isNotEmpty()) {
                    val serviceIntent = Intent(context, GhostVpnService::class.java).apply {
                        this.action = GhostVpnService.ACTION_CONNECT
                        putExtra(GhostVpnService.EXTRA_PROFILE_ID, activeProfileId)
                    }
                    context.startForegroundService(serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
