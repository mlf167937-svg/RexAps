package com.rexaps.rexpanel

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun RexPanelScreen(
    activity: Activity,
    onExit: () -> Unit
) {
    RexPanelScreenContent(activity = activity, onExit = onExit)
}
