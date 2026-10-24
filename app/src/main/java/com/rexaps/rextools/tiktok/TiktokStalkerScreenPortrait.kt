// TiktokStalkerScreen.kt

package com.rexaps.rextools.tiktok

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.ui.*

@Composable
fun TiktokStalkerScreenPortrait(
    onBack: () -> Unit = {},
    vm: TiktokStalkerViewModel = viewModel()
) {
    TiktokStalkerScreenPortraitContent(onBack = onBack, vm = vm)
}
