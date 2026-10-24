package com.rexaps.rexspacex

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun RexSpaceXGamingTools(accent: Color) {
    val context = LocalContext.current
        SectionTitle("GAMING TOOLS", "04")
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ToolTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.NetworkCheck,
                title = "Network",
                subtitle = "Open settings",
                accent = accent
            ) {
                try {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    toast(context, "Pengaturan jaringan tidak tersedia")
                }
            }
            ToolTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MusicNote,
                title = "Music",
                subtitle = "Audio controls",
                accent = accent
            ) {
                try {
                    context.startActivity(Intent("android.intent.action.MUSIC_PLAYER").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    toast(context, "Tidak ada pemutar musik yang tersedia")
                }
            }
            ToolTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Settings,
                title = "System",
                subtitle = "Display settings",
                accent = accent
            ) {
                try {
                    context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    toast(context, "Pengaturan layar tidak tersedia")
                }
            }
        }
}
