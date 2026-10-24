
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun RexPushSuccessAnimation() {
    val transition = rememberInfiniteTransition(label = "rexPushSuccess")
    val ring by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successRing"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(420),
        label = "successIcon"
    )

    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = scheme.primary.copy(alpha = alpha),
                radius = size.minDimension * 0.38f * ring,
                center = androidx.compose.ui.geometry.Offset(
                    size.width / 2f,
                    size.height / 2f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx()
                )
            )

            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.43f

            listOf(0f, 60f, 120f, 180f, 240f, 300f).forEach { deg ->
                val a = Math.toRadians(deg.toDouble())
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.28f),
                    radius = 2.5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        cx + cos(a).toFloat() * r,
                        cy + sin(a).toFloat() * r
                    )
                )
            }
        }

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier
                .size(62.dp)
                .scale(iconScale)
        )
    }
}

@Composable
internal fun ButtonSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp
    )
}
