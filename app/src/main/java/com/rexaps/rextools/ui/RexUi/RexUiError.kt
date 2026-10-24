// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun RexErrorSlot(error: String?, title: String) {
    var last by remember { mutableStateOf("") }
    SideEffect { if (error != null) last = error }
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(spring(0.8f)),
        exit = fadeOut() + shrinkVertically()
    ) {
        RexErrorBanner(title, error ?: last)
    }
}
