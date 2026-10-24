// PriviewScreen.kt

package com.rexaps.rextools.preview

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.ui.*

@Composable
fun PreviewScreenPortrait(
    onBack: () -> Unit = {},
    vm: PreviewViewModel = viewModel()
) {
    PreviewScreenPortraitContent(onBack = onBack, vm = vm)
}
