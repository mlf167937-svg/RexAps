package com.rexaps.rexwarp.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.*
import com.rexaps.rexwarp.tunnel.RexWarpTunnelCapabilities

@Composable
fun RexWarpSettingsScreen(
    settings: RexWarpSettings, capabilities: RexWarpTunnelCapabilities, engineReason: String?,
    onChange: (RexWarpSettings) -> Unit, onExport: () -> Unit, onImport: () -> Unit,
    onResetToday: () -> Unit, onResetAll: () -> Unit, onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section("Connection") {
            SwitchRow("Auto-reconnect", "Reconnect automatically after an unexpected disconnect.", settings.autoConnect) { onChange(settings.copy(autoConnect = it)) }
            SwitchRow("Connect on boot", "Start RexWARP after the device restarts. Requires VPN permission to be granted already.", settings.connectOnBoot) { onChange(settings.copy(connectOnBoot = it)) }
        }
        Section("Network rules") {
            SwitchRow("Pause on metered network", "Disconnect while the active network is metered.", settings.pauseOnMetered) { onChange(settings.copy(pauseOnMetered = it)) }
            SwitchRow("Pause on Wi-Fi", "Disconnect while on Wi-Fi.", settings.pauseOnWifi) { onChange(settings.copy(pauseOnWifi = it)) }
            SwitchRow("Pause on mobile data", "Disconnect while on mobile data.", settings.pauseOnMobile) { onChange(settings.copy(pauseOnMobile = it)) }
        }
        Section("IP") {
            SwitchRow("IPv4", "At least one IP version must stay enabled.", settings.ipv4, enabled = settings.ipv6 || !settings.ipv4 ) { onChange(settings.copy(ipv4 = it)) }
            SwitchRow(
                "IPv6", if (capabilities.supportsIpv6) "Tunnel IPv6 traffic." else "Requires a tunnel engine with IPv6 support.",
                settings.ipv6, enabled = capabilities.supportsIpv6 && (settings.ipv4 || !settings.ipv6)
            ) { onChange(settings.copy(ipv6 = it)) }
        }
        DnsSection(settings, onChange)
        MtuSection(settings, capabilities.supportsMtu, onChange)
        Section("Kill switch") {
            SwitchRow(
                "Kill switch",
                if (capabilities.supportsKillSwitch) "Block traffic if the tunnel drops."
                else "Not available: it needs tunnel-engine support to be implemented safely.",
                settings.killSwitch, enabled = capabilities.supportsKillSwitch
            ) { onChange(settings.copy(killSwitch = it)) }
            Text("You can use Android's own protection: VPN settings → RexAps → Block connections without VPN.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Open system VPN settings") }
        }
        Section("Data") {
            Text("Usage data stays on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport) { Text("Export JSON") }
                OutlinedButton(onClick = onImport) { Text("Import JSON") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { confirm = "today" }) { Text("Reset today") }
                OutlinedButton(onClick = { confirm = "all" }) { Text("Reset all") }
            }
        }
        OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) { Text("About & privacy") }
        if (engineReason != null) Text("Engine status: $engineReason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    confirm?.let { which ->
        val all = which == "all"
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (all) "Reset all statistics?" else "Reset today's statistics?") },
            text = { Text(if (all) "All saved usage history will be permanently deleted. Export first if you want a backup." else "Today's recorded traffic will be permanently deleted.") },
            confirmButton = { TextButton(onClick = { if (all) onResetAll() else onResetToday(); confirm = null }) { Text(if (all) "Delete all" else "Delete today") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DnsSection(settings: RexWarpSettings, onChange: (RexWarpSettings) -> Unit) {
    var text by rememberSaveable(settings.customDns) { mutableStateOf(settings.customDns) }
    val parsed = RexWarpDns.parseCustom(text)
    Section("DNS") {
        RexWarpDnsMode.entries.forEach { mode ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = settings.dnsMode == mode, role = Role.RadioButton) { onChange(settings.copy(dnsMode = mode)) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = settings.dnsMode == mode, onClick = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    if (mode == RexWarpDnsMode.CLOUDFLARE) Text("1.1.1.1, 1.0.0.1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (settings.dnsMode == RexWarpDnsMode.CUSTOM) {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("DNS servers") }, singleLine = true, isError = text.isNotBlank() && parsed == null,
                supportingText = { Text(if (text.isNotBlank() && parsed == null) "Enter 1–4 valid IP addresses separated by commas." else "Example: 9.9.9.9, 149.112.112.112") }
            )
            Button(enabled = parsed != null && text != settings.customDns, onClick = { onChange(settings.copy(customDns = parsed!!.joinToString(", "))) }) { Text("Save DNS") }
        }
        Text("DNS settings are applied when the tunnel connects.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MtuSection(settings: RexWarpSettings, supported: Boolean, onChange: (RexWarpSettings) -> Unit) {
    var text by rememberSaveable(settings.mtu) { mutableStateOf(settings.mtu?.toString().orEmpty()) }
    val value = text.toIntOrNull()
    val valid = text.isBlank() || value in 1280..1500
    Section("MTU") {
        OutlinedTextField(
            value = text, onValueChange = { text = it.filter(Char::isDigit).take(4) }, enabled = supported,
            modifier = Modifier.fillMaxWidth(), label = { Text("MTU (empty = automatic)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !valid,
            supportingText = { Text(if (!supported) "Requires a tunnel engine with MTU support." else if (!valid) "Use a value from 1280 to 1500." else "Range 1280–1500") }
        )
        Button(enabled = supported && valid && value != settings.mtu, onClick = { onChange(settings.copy(mtu = value)) }) { Text("Save MTU") }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}