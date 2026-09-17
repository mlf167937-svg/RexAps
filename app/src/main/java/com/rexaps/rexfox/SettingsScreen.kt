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
        Row(Modifier.fillMaxWidth().background(SurfaceDark).padding(10.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Settings", fontWeight = FontWeight.Bold)
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Section("GENERAL")
            Surface(Modifier.fillMaxWidth(), color = SurfaceCard, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Search engine", fontWeight = FontWeight.SemiBold)
                    SearchEngine.values().forEach { engine ->
                        TextButton(onClick = { onChange(settings.copy(searchEngine = engine)) }) {
                            Text(if (engine == settings.searchEngine) "✓ ${engine.label}" else engine.label)
                        }
                    }
                }
            }

            Section("PRIVACY")
            Toggle("Tracking protection", "No tracker blocker is installed in this build.", settings.trackingProtectionEnabled) {
                onChange(settings.copy(trackingProtectionEnabled = it))
            }
            Toggle("Do Not Track", "Request the preference where supported.", settings.doNotTrack) {
                onChange(settings.copy(doNotTrack = it))
            }
            OutlinedButton(onClick = onClearHistory, Modifier.fillMaxWidth()) {
                Text("Clear browsing history")
            }

            Section("WEB")
            Toggle("JavaScript", "Required by many modern websites.", settings.javaScriptEnabled) {
                onChange(settings.copy(javaScriptEnabled = it))
            }
            Toggle("DOM storage", "Allow website local storage.", settings.domStorageEnabled) {
                onChange(settings.copy(domStorageEnabled = it))
            }
            Toggle("Third-party cookies", "Allow embedded services to use cookies.", settings.thirdPartyCookiesEnabled) {
                onChange(settings.copy(thirdPartyCookiesEnabled = it))
            }
            Toggle("Desktop site", "Request desktop layouts.", settings.desktopSite) {
                onChange(settings.copy(desktopSite = it))
            }
        }
    }
}

@Composable private fun Section(text: String) =
    Text(text, color = OnSurfaceMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = SurfaceCard, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = OnSurfaceMuted, fontSize = 11.sp)
            }
            Switch(checked, onCheckedChange = onChange)
        }
    }
}
