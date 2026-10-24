package com.rexaps.rexpanel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun GaugeCard(
    title: String,
    fraction: Float?,
    value: String,
    sub: String,
    history: List<Float>?,
    modifier: Modifier
) {
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(24.dp)

    val f =
        (fraction ?: 0f)
            .coerceIn(0f, 1f)

    val sweep by animateFloatAsState(
        f * 270f,
        tween(700),
        label = "gauge"
    )

    val arcColor =
        when {
            f < 0.6f ->
                colors.primary

            f < 0.85f ->
                Warn

            else ->
                Bad
        }

    val level =
        when {
            fraction == null ->
                "tidak tersedia"

            f < 0.6f ->
                "normal"

            f < 0.85f ->
                "tinggi"

            else ->
                "kritis"
        }

    val track =
        colors.outlineVariant

    val lineColor =
        colors.tertiary

    Column(
        modifier
            .clip(shape)
            .background(
                colors.surfaceVariant
            )
            .border(
                1.dp,
                colors.outlineVariant,
                shape
            )
            .padding(14.dp)
            .semantics(
                mergeDescendants = true
            ) {
                contentDescription =
                    "$title $value, $level. $sub"
            },

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            title,
            style =
                MaterialTheme.typography
                    .labelMedium,
            color =
                colors.onSurfaceVariant
        )

        Spacer(
            Modifier.height(6.dp)
        )

        Box(
            Modifier.size(100.dp),
            contentAlignment =
                Alignment.Center
        ) {

            Canvas(
                Modifier.fillMaxSize()
            ) {

                val stroke =
                    10.dp.toPx()

                val arcSize =
                    Size(
                        size.width - stroke,
                        size.height - stroke
                    )

                val topLeft =
                    Offset(
                        stroke / 2,
                        stroke / 2
                    )

                drawArc(
                    track,
                    135f,
                    270f,
                    false,
                    topLeft,
                    arcSize,
                    style =
                        Stroke(
                            stroke,
                            cap =
                                StrokeCap.Round
                        )
                )

                if (sweep > 0.5f) {

                    drawArc(
                        arcColor,
                        135f,
                        sweep,
                        false,
                        topLeft,
                        arcSize,
                        style =
                            Stroke(
                                stroke,
                                cap =
                                    StrokeCap.Round
                            )
                    )
                }
            }

            Text(
                value,
                style =
                    MaterialTheme.typography
                        .titleLarge
            )
        }

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            sub,
            style =
                MaterialTheme.typography
                    .labelSmall,
            color =
                colors.onSurfaceVariant,
            maxLines = 1
        )

        if (
            history != null &&
            history.size >= 2
        ) {

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(28.dp)
            ) {

                val path =
                    Path()

                history.forEachIndexed {
                    i,
                    v ->

                    val x =
                        size.width *
                            i /
                            (history.size - 1)

                    val y =
                        size.height *
                            (
                                1f -
                                    v.coerceIn(
                                        0f,
                                        1f
                                    )
                                )

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path,
                    lineColor,
                    style =
                        Stroke(
                            2.dp.toPx(),
                            cap =
                                StrokeCap.Round
                        )
                )
            }
        }
    }
}
