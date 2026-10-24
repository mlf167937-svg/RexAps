//EditorArea.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rexaps.rexcoder.theme.Rex

@Composable fun HDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Rex.Border))

@Composable fun VDivider() = Box(Modifier.fillMaxHeight().width(1.dp).background(Rex.Border))

/** Thickness of the touch area of every draggable divider. */
val HANDLE_THICKNESS: Dp = 12.dp
