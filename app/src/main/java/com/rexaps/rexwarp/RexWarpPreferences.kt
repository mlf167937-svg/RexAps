package com.rexaps.rexwarp

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.rexaps.rexwarp.tunnel.RexWarpTunnelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.InetAddress

private val Context.rexWarpStore by preferencesDataStore("rexwarp_settings")

enum class RexWarpDnsMode(val label: String) { CLOUDFLARE("Cloudflare DNS"), CUSTOM("Custom DNS"), SYSTEM("System DNS") }

data class RexWarpSettings(
    val autoConnect: Boolean = false,      // reconnect otomatis setelah putus tak terduga
    val connectOnBoot: Boolean = false,
    val pauseOnMetered: Boolean = false,
    val pauseOnWifi: Boolean = false,
    val pauseOnMobile: Boolean = false,
    val ipv4: Boolean = true,
    val ipv6: Boolean = false,
    val dnsMode: RexWarpDnsMode = RexWarpDnsMode.CLOUDFLARE,
    val customDns: String = "",
    val mtu: Int? = null,
    val killSwitch: Boolean = false
)

object RexWarpDns {
    val CLOUDFLARE_V4 = listOf("1.1.1.1", "1.0.0.1")
    val CLOUDFLARE_V6 = listOf("2606:4700:4700::1111", "2606:4700:4700::1001")
    private val V4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val V6 = Regex("""^[0-9a-fA-F:.]+$""")

    fun isValidIp(s: String): Boolean = when {
        V4.matches(s) -> s.split('.').all { it.toInt() <= 255 }
        ':' in s && V6.matches(s) -> runCatching { InetAddress.getByName(s) }.isSuccess // literal, tanpa lookup
        else -> false
    }

    /** null jika kosong / tidak valid. Maksimal 4 server. */
    fun parseCustom(text: String): List<String>? {
        val items = text.split(',', ' ', '\n', ';').map { it.trim() }.filter { it.isNotEmpty() }
        return items.takeIf { it.size in 1..4 && it.all(::isValidIp) }
    }
}

fun RexWarpSettings.toTunnelConfig(): RexWarpTunnelConfig? {
    if (!ipv4 && !ipv6) return null
    if (mtu != null && mtu !in 1280..1500) return null
    val dns = when (dnsMode) {
        RexWarpDnsMode.CLOUDFLARE -> RexWarpDns.CLOUDFLARE_V4 + if (ipv6) RexWarpDns.CLOUDFLARE_V6 else emptyList()
        RexWarpDnsMode.CUSTOM -> RexWarpDns.parseCustom(customDns) ?: return null
        RexWarpDnsMode.SYSTEM -> emptyList()
    }
    return RexWarpTunnelConfig(dns, ipv4, ipv6, mtu, killSwitch)
}

class RexWarpPreferences(private val context: Context) {
    private object K {
        val auto = booleanPreferencesKey("auto_connect"); val boot = booleanPreferencesKey("connect_on_boot")
        val metered = booleanPreferencesKey("pause_metered"); val wifi = booleanPreferencesKey("pause_wifi")
        val mobile = booleanPreferencesKey("pause_mobile"); val v4 = booleanPreferencesKey("ipv4")
        val v6 = booleanPreferencesKey("ipv6"); val dnsMode = stringPreferencesKey("dns_mode")
        val customDns = stringPreferencesKey("custom_dns"); val mtu = intPreferencesKey("mtu")
        val kill = booleanPreferencesKey("kill_switch")
    }

    val settings: Flow<RexWarpSettings> = context.rexWarpStore.data.map { p ->
        RexWarpSettings(
            autoConnect = p[K.auto] ?: false, connectOnBoot = p[K.boot] ?: false,
            pauseOnMetered = p[K.metered] ?: false, pauseOnWifi = p[K.wifi] ?: false,
            pauseOnMobile = p[K.mobile] ?: false, ipv4 = p[K.v4] ?: true, ipv6 = p[K.v6] ?: false,
            dnsMode = runCatching { RexWarpDnsMode.valueOf(p[K.dnsMode] ?: "") }.getOrDefault(RexWarpDnsMode.CLOUDFLARE),
            customDns = p[K.customDns] ?: "", mtu = p[K.mtu], killSwitch = p[K.kill] ?: false
        )
    }

    suspend fun save(s: RexWarpSettings) {
        context.rexWarpStore.edit { p ->
            p[K.auto] = s.autoConnect; p[K.boot] = s.connectOnBoot; p[K.metered] = s.pauseOnMetered
            p[K.wifi] = s.pauseOnWifi; p[K.mobile] = s.pauseOnMobile; p[K.v4] = s.ipv4; p[K.v6] = s.ipv6
            p[K.dnsMode] = s.dnsMode.name; p[K.customDns] = s.customDns; p[K.kill] = s.killSwitch
            if (s.mtu != null) p[K.mtu] = s.mtu else p.remove(K.mtu)
        }
    }
}