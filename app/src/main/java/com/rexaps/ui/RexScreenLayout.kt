package com.rexaps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Layout intent supplied by each Screen.kt orientation dispatcher. */
enum class RexScreenLayoutMode { PORTRAIT, LANDSCAPE }

val LocalRexScreenLayoutMode = staticCompositionLocalOf { RexScreenLayoutMode.PORTRAIT }

@Composable
fun rexIsLandscapeLayout(): Boolean = LocalRexScreenLayoutMode.current == RexScreenLayoutMode.LANDSCAPE
