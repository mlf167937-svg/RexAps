package com.rexaps.rexmanager

import androidx.compose.runtime.Composable

@Composable
fun RexManagerScreen(onExit: () -> Unit = {}) {
    RexManagerScreenContent(onExit = onExit)
}
