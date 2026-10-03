package com.rexaps.rexgit

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class RexGitAuthStore(
    context: Context
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            "rexgit_auth",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEY_ALIAS = "rexgit_github_key"
        private const val USERNAME = "username"
        private const val TOKEN = "token"
        private const val IV_SIZE = 12
    }

    fun save(username: String, token: String) {
        prefs.edit()
            .putString(USERNAME, username)
            .putString(TOKEN, encrypt(token))
            .apply()
    }

    fun load(): RexGitAuth? {
        val username = prefs.getString(USERNAME, null) ?: return null
        val encrypted = prefs.getString(TOKEN, null) ?: return null

        return try {
            RexGitAuth(username, decrypt(encrypted))
        } catch (_: Throwable) {
            // Key was lost or data is corrupted: start clean instead of crashing.
            prefs.edit().clear().apply()
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )

        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())

        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val iv = cipher.iv

        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val data = Base64.decode(value, Base64.NO_WRAP)
        require(data.size > IV_SIZE) { "Corrupted token" }

        val iv = data.copyOfRange(0, IV_SIZE)
        val encrypted = data.copyOfRange(IV_SIZE, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))

        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }
}
