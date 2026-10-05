package com.rexaps.rexwarp.tunnel

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.wireguard.crypto.KeyPair
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class RexWarpAccount(
    val id: String,
    val token: String,
    val privateKey: String,
    val peerPublicKey: String,
    val endpointHost: String,
    val endpointPort: Int,
    val ipv4: String,
    val ipv6: String?
)

class RexWarpRegistrationException(message: String, cause: Throwable? = null) : IOException(message, cause)

private fun RexWarpAccount.toJson() = JSONObject()
    .put("id", id).put("token", token).put("privateKey", privateKey)
    .put("peerPublicKey", peerPublicKey).put("endpointHost", endpointHost)
    .put("endpointPort", endpointPort).put("ipv4", ipv4).put("ipv6", ipv6 ?: "")

private fun accountFromJson(o: JSONObject) = RexWarpAccount(
    id = o.getString("id"), token = o.getString("token"), privateKey = o.getString("privateKey"),
    peerPublicKey = o.getString("peerPublicKey"), endpointHost = o.getString("endpointHost"),
    endpointPort = o.getInt("endpointPort"), ipv4 = o.getString("ipv4"),
    ipv6 = o.optString("ipv6").ifBlank { null }
)

/** Menyimpan akun (termasuk private key) terenkripsi AES-GCM dengan kunci di Android Keystore. */
class RexWarpAccountStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "rexwarp_account.bin")

    fun load(): RexWarpAccount? = runCatching {
        val raw = file.readBytes()
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, IV_SIZE)))
        accountFromJson(JSONObject(String(cipher.doFinal(raw, IV_SIZE, raw.size - IV_SIZE))))
    }.getOrNull()

    fun save(account: RexWarpAccount) {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        file.writeBytes(cipher.iv + cipher.doFinal(account.toJson().toString().toByteArray()))
    }

    fun clear() { file.delete() }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build()
        )
        return gen.generateKey()
    }

    private companion object {
        const val ALIAS = "rexwarp_account_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}

/**
 * Mendaftarkan akun WARP gratis (endpoint tidak resmi Cloudflare; sama seperti tool wgcf).
 * Blocking: panggil dari IO. Mencoba beberapa versi API bila yang pertama ditolak.
 */
object RexWarpRegistration {
    /** IP engage.cloudflareclient.com. Dipakai langsung supaya handshake tidak bergantung DNS. */
    const val ENDPOINT_HOST = "162.159.192.1"
    const val ENDPOINT_PORT = 2408
    private val API_VERSIONS = listOf("v0a1922", "v0a2158")

    fun register(): RexWarpAccount {
        var last: Exception? = null
        for (v in API_VERSIONS) {
            try {
                return registerAt("https://api.cloudflareclient.com/$v/reg")
            } catch (e: Exception) {
                last = e
            }
        }
        throw RexWarpRegistrationException("WARP registration failed: ${last?.message}", last)
    }

    private fun open(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("User-Agent", "okhttp/3.12.1")
            setRequestProperty("CF-Client-Version", "a-6.3-1922")
        }

    private fun registerAt(base: String): RexWarpAccount {
        val pair = KeyPair()
        val body = JSONObject()
            .put("key", pair.publicKey.toBase64())
            .put("install_id", "").put("fcm_token", "")
            .put("tos", Instant.now().toString())
            .put("type", "Android").put("model", "PC").put("locale", "en_US")

        val conn = open(base, "POST").apply { doOutput = true }
        val root: JSONObject
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }

        val id = root.getString("id")
        val token = root.optString("token")
        val config = root.getJSONObject("config")
        val peer = config.getJSONArray("peers").getJSONObject(0)
        val addresses = config.getJSONObject("interface").getJSONObject("addresses")

        // Aktifkan device seperti wgcf. Best effort: gagal tidak membatalkan pendaftaran.
        runCatching {
            val c = open("$base/$id", "PATCH").apply {
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                c.outputStream.use { it.write("""{"warp_enabled":true}""".toByteArray()) }
                c.responseCode
            } finally {
                c.disconnect()
            }
        }

        return RexWarpAccount(
            id = id, token = token,
            privateKey = pair.privateKey.toBase64(),
            peerPublicKey = peer.getString("public_key"),
            endpointHost = ENDPOINT_HOST, endpointPort = ENDPOINT_PORT,
            ipv4 = addresses.getString("v4"),
            ipv6 = addresses.optString("v6").ifBlank { null }
        )
    }
}
