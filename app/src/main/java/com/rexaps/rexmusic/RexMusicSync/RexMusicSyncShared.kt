package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

internal const val TRANSFER_PORT = 38742

internal const val HEARTBEAT_MS = 800L

internal const val CLIENT_TIMEOUT_MS = 3500L

internal const val START_LEAD_MS = 3000L

internal const val DRIFT_CORRECTION_MS = 250L
