package com.rexaps.ui

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun PermissionGate(padding: PaddingValues, onAllow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "gate")
    val pulse by inf.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val steps = if (Build.VERSION.SDK_INT >= 30) {
        listOf(
            "Tekan tombol \"Izinkan akses\" di bawah",
            "Aktifkan \"Izinkan akses ke semua file\"",
            "Kembali ke RexAps, halaman akan terbuka otomatis"
        )
    } else {
        listOf(
            "Tekan tombol \"Izinkan akses\" di bawah",
            "Setujui izin penyimpanan yang muncul"
        )
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
        Aurora(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(136.dp).clip(CircleShape).background(colors.primary.copy(alpha = .12f)))
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = colors.onPrimary, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Izin penyimpanan diperlukan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Profil, story, dan video kamu disimpan langsung di Download/RexAps/.config/ — 100% lokal, tanpa server.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    steps.forEachIndexed { i, s -> PermissionStep(i + 1, s) }
                }
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onAllow,
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Izinkan akses", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun PermissionStep(number: Int, text: String) {
    val c = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(c.primary.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = c.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
    }
}
