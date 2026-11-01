package com.rexaps.rexai

import android.os.Bundle
import androidx.lifecycle.ViewModel

/** Keeps WebView navigation state across Activity configuration changes. */
class RexAIWebStateHolder : ViewModel() {
    private val savedStates = mutableMapOf<String, Bundle>()

    @Synchronized
    fun takeState(provider: String): Bundle? = savedStates.remove(provider)?.let { Bundle(it) }

    @Synchronized
    fun saveState(provider: String, state: Bundle) {
        savedStates[provider] = Bundle(state)
    }
}
