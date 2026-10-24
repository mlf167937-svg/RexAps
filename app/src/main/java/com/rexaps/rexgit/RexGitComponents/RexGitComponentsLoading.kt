
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Reserves space for the progress bar so the layout never jumps. */
@Composable
internal fun ProgressSlot(visible: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        if (visible) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
