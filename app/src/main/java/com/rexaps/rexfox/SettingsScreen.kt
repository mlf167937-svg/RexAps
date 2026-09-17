package com.rexaps.rexfox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearHistory: () -> Unit
) {
    var jsEnabled by remember { mutableStateOf(true) }
    var domStorage by remember { mutableStateOf(true) }
    var trackingProtection by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = OnSurfacePrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsSectionLabel("Privacy")

            SettingsToggle(
                icon = Icons.Default.Shield,
                title = "Tracking Protection",
                subtitle = "Block known trackers",
                checked = trackingProtection,
                onCheckedChange = { trackingProtection = it }
            )

            SettingsSectionLabel("Web Engine")

            SettingsToggle(
                icon = Icons.Default.Code,
                title = "JavaScript",
                subtitle = "Enable JS execution",
                checked = jsEnabled,
                onCheckedChange = { jsEnabled = it }
            )

            SettingsToggle(
                icon = Icons.Default.Storage,
                title = "DOM Storage",
                subtitle = "Allow local storage",
                checked = domStorage,
                onCheckedChange = { domStorage = it }
            )

            SettingsSectionLabel("Data")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = Color(0xFF200D0D)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Clear History", fontWeight = FontWeight.SemiBold, color = OnSurfacePrimary)
                        Text("Remove all browsing history", fontSize = 12.sp, color = OnSurfaceMuted)
                    }
                    TextButton(onClick = onClearHistory) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("RexFox v1.0 — Private · Fast · Dev-Ready",
                fontSize = 11.sp, color = OnSurfaceMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun SettingsSectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = OnSurfaceMuted, letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = VioletLight, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = OnSurfacePrimary, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = OnSurfaceMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Violet
                )
            )
        }
    }
}
