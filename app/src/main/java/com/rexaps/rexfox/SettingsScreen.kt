package com.rexaps.rexfox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    onChange: (BrowserSettings) -> Unit,
    onBack: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(DeepBlack)) {
        Row(
            Modifier.fillMaxWidth().background(SurfaceDark).statusBarsPadding().padding(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Settings", fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Section("GENERAL")
            Surface(Modifier.fillMaxWidth(), color = SurfaceCard, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Search engine", fontWeight = FontWeight.SemiBold)
                    SearchEngine.entries.forEach { engine ->
                        TextButton(onClick = { onChange(settings.copy(searchEngine = engine)) }) {
                            Text(if (engine == settings.searchEngine) "✓ ${engine.label}" else engine.label)
                        }
                    }
                }
            }

            Section("PRIVACY & SECURITY")
            Toggle(
                "Tracking protection",
                "Uses GeckoView's built-in tracking protection for browser sessions.",
                settings.trackingProtectionEnabled
            ) { onChange(settings.copy(trackingProtectionEnabled = it)) }
            InfoRow(
                "Do Not Track",
                "The selected GeckoView API does not expose the legacy WebView Do Not Track switch. The stored preference is retained but is not sent as a request header."
            )
            OutlinedButton(onClick = onClearHistory, Modifier.fillMaxWidth()) {
                Text("Clear browsing history")
            }

            Section("WEB CONTENT")
            Toggle("JavaScript", "Required by many modern websites.", settings.javaScriptEnabled) {
                onChange(settings.copy(javaScriptEnabled = it))
            }
            Toggle("Desktop site", "Request desktop layouts from websites.", settings.desktopSite) {
                onChange(settings.copy(desktopSite = it))
            }
            InfoRow(
                "DOM storage",
                "Managed by GeckoView and enabled by default for regular sessions. Private sessions use private browsing mode."
            )
            InfoRow(
                "Third-party cookies",
                "Cookie policy is managed by GeckoView. This API version does not expose the previous per-WebView cookie toggle. Normal profiles remain isolated from private browsing."
            )
        }
    }
}

@Composable
private fun Section(text: String) =
    Text(text, color = OnSurfaceMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = SurfaceCard, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = OnSurfaceMuted, fontSize = 11.sp)
            }
            Switch(checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun InfoRow(title: String, description: String) {
    Surface(Modifier.fillMaxWidth(), color = SurfaceCard, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = OnSurfaceMuted, fontSize = 11.sp)
        }
    }
}
