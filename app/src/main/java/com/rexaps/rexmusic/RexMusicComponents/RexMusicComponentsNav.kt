package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

/** Target menu opsi lagu. onPlay = null berarti menu "Putar" disembunyikan. */
class MenuTarget(val track: RexTrack, val onPlay: (() -> Unit)?)
