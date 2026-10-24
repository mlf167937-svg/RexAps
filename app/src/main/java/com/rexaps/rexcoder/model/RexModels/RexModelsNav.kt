package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

internal fun RexCoderState.__splitImpl_closeTab(g: EditorGroup, path: String) {
        val i = g.tabs.indexOf(path); if (i < 0) return
        g.tabs.removeAt(i)
        if (g.active == path) g.active = g.tabs.getOrNull(minOf(i, g.tabs.lastIndex))
        if (g.tabs.isEmpty() && groups.size > 1) removeGroup(g.id)
    }
