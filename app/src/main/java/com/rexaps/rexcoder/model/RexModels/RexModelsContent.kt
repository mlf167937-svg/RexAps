package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

@Composable
internal fun __splitImpl_rememberRexCoderState(): RexCoderState = remember { RexCoderState().also { it.expanded.add(it.root.path) } }
