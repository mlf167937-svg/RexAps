package com.rexaps.rexadm

import android.content.Context
import android.content.Intent
import com.rexaps.librex.rexsmartmanager.OkHttpRexSmartManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide RexADM owner shared by the internal module screen and its intent Activity. */
object RexAdmRuntime {
    @Volatile private var instance: OkHttpRexSmartManager? = null
    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl = _pendingUrl.asStateFlow()

    fun manager(context: Context): OkHttpRexSmartManager = instance ?: synchronized(this) {
        instance ?: OkHttpRexSmartManager(context.applicationContext).also { instance = it }
    }

    fun acceptIntent(intent: Intent?) {
        val raw = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.trim().orEmpty()
        val match = Regex("https?://\\S+", RegexOption.IGNORE_CASE).find(raw)?.value
        val url = match?.trimEnd('.', ',', ')', ']', '}', '>', '"', '\'').orEmpty()
        if (url.isNotBlank()) _pendingUrl.value = url
    }

    fun clearPendingUrl() { _pendingUrl.value = null }
}
