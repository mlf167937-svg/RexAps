package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PrivacyPanel() {
    val c = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(c.secondary.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Shield, null, tint = c.secondary) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Transparansi & privasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Ketahui cara hasil tes diproses", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge, color = c.primary)
            }
            AnimatedVisibility(expanded, enter = fadeIn(tween(180)), exit = fadeOut(tween(140))) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tes memakai mesin pengukuran Cloudflare yang dimuat melalui internet. Penggunaan data bisa mencapai puluhan MB, tergantung konfigurasi tes dan kondisi jaringan.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    Text("Jika RexWarp VPN aktif, hasil dapat mencerminkan rute koneksi melalui VPN. Jalankan tes beberapa kali untuk membandingkan hasil dengan lebih adil.", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
        }
    }
}
