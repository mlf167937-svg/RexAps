package com.rexaps.rexchat.v1

/** JNI bridge for the Android-built libnode.so. Node can only be started once per app process. */
internal object RexNodeBridge {
    init {
        System.loadLibrary("node")
        System.loadLibrary("rexnode")
    }

    external fun startNodeWithArguments(arguments: Array<String>): Int
}
