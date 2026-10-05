package com.rexaps.rexwarp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.rexaps.rexwarp.tunnel.RexWarpTunnelRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RexWarpBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (RexWarpBackupStore.get(context).recording) {
            runCatching { RexWarpRecorderService.start(context) }
        }

        val pending = goAsync()
        val repo = RexWarpRepository.get(context)
        repo.appScope.launch {
            try {
                val s = repo.preferences.settings.first()
                val ready = RexWarpTunnelRegistry.provider.unavailableReason == null &&
                    VpnService.prepare(context) == null // izin VPN sudah diberikan sebelumnya
                if (s.connectOnBoot && ready) RexWarpVpnService.start(context)
            } finally { pending.finish() }
        }
    }
}
