// RexLocalAccountScreen.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

suspend internal fun RexProfileController.__split_load_1() {
        withContext(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            RexPaths.video.mkdirs()
        }
        withContext(Dispatchers.IO) { readAccount() }?.let { acct = it; rev++ }
        videos = withContext(Dispatchers.IO) { loadVideos() }
        visible = true
        while (true) {
            stories = withContext(Dispatchers.IO) { loadStories() }
            delay(60_000)
        }
    }
