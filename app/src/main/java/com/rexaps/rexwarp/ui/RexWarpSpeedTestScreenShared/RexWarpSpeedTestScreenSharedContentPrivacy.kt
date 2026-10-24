package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun MetricTile(modifier: Modifier, title: String, value: String, unit: String, accent: Color, icon: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(modifier.animateContentSize(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides accent) { icon() }
                }
                Text(title, style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                AnimatedContent(value, label = "metric-$title") { Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                Text(unit, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
internal fun ErrorPanel(message: String) {
    val c = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.errorContainer)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, null, tint = c.onErrorContainer)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pengujian belum berhasil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.onErrorContainer)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
            }
        }
    }
}

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
