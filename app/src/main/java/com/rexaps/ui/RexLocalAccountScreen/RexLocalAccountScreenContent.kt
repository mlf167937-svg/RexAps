// RexLocalAccountScreen.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier,
        shape = RoundedCornerShape(22.dp),
        color = c.surface.copy(alpha = .55f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = .5f))
    ) { Column(content = content) }
}
