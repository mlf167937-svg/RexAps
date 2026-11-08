package com.rexaps.rexchat.v1

import android.util.Log

/** JNI bridge for the Android-built Node.js 26.13 runtime. Node may only be started once per app process. */
internal object RexNodeBridge {
    private const val TAG = "RexNodeBridge"

    init {
        // Load packaged dependencies explicitly so a missing dependency is reported
        // by its real library name before libnode is initialized.
        listOf("c++_shared", "cares", "sqlite3", "node", "rexnode").forEach { library ->
            try {
                System.loadLibrary(library)
                Log.i(TAG, "Loaded lib$library.so")
            } catch (error: Throwable) {
                throw UnsatisfiedLinkError("Unable to load lib$library.so: ${error.message}").apply {
                    initCause(error)
                }
            }
        }
    }

    external fun startNodeWithArguments(arguments: Array<String>): Int
}
