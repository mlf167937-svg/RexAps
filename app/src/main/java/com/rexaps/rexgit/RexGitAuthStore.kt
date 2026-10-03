package com.rexaps.rexgit

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

data class RexGitAuth(
    val username: String,
    val token: String
)

class RexGitAuthStore(
    context: Context
) {
    private val prefs = context.getSharedPreferences(
        "rexgit_auth",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_ALIAS = "rexgit_github_key"
        private const val TOKEN = "token"
        private const val USERNAME = "username"
    }

    fun save(
        username: String,
        token: String
    ) {
        val encrypted = encrypt(token)

        prefs.edit()
            .putString(USERNAME, username)
            .putString(TOKEN, encrypted)
            .apply()
    }

    fun load(): RexGitAuth? {
        val username = prefs.getString(USERNAME, null)
            ?: return null

        val encrypted = prefs.getString(TOKEN, null)
            ?: return null

        return try {
            RexGitAuth(
                username = username,
                token = decrypt(encrypted)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun getKey(): SecretKey {
        val keyStore = java.security.KeyStore
            .getInstance("AndroidKeyStore")
            .apply {
                load(null)
            }

        val existing = keyStore.getKey(
            KEY_ALIAS,
            null
        ) as? SecretKey

        if (existing != null) {
            return existing
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()
        )

        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getKey()
        )

        val encrypted = cipher.doFinal(
            value.toByteArray(StandardCharsets.UTF_8)
        )

        val iv = cipher.iv

        val combined = ByteArray(
            iv.size + encrypted.size
        )

        System.arraycopy(
            iv,
            0,
            combined,
            0,
            iv.size
        )

        System.arraycopy(
            encrypted,
            0,
            combined,
            iv.size,
            encrypted.size
        )

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(
            value,
            Base64.NO_WRAP
        )

        val ivSize = 12

        val iv = combined.copyOfRange(
            0,
            ivSize
        )

        val encrypted = combined.copyOfRange(
            ivSize,
            combined.size
        )

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getKey(),
            GCMParameterSpec(
                128,
                iv
            )
        )

        return String(
            cipher.doFinal(encrypted),
            StandardCharsets.UTF_8
        )
    }
}
