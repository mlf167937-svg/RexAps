package com.rexaps.gecko

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import org.mozilla.geckoview.GeckoRuntime

/** One application-process Gecko runtime shared by RexFox and RexAI. Sessions remain isolated. */
object RexGeckoRuntime {
    @Volatile private var instance: GeckoRuntime? = null

    @MainThread
    fun get(context: Context): GeckoRuntime {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "GeckoRuntime must be initialized on the Android main thread."
        }
        return instance ?: synchronized(this) {
            instance ?: GeckoRuntime.create(context.applicationContext).also { instance = it }
        }
    }
}
