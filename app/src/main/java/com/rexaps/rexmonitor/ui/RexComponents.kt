package com.rexaps.rexmonitor.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

const val REX_UNAVAILABLE = "Tidak tersedia"

@Composable
fun RexCard(
    modifier: Modifier = Modifier,
    accent: Color,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = RexTheme.palette
    val shape = RoundedCornerShape(20.dp)
    val semanticsModifier = if (description != null) {
        Modifier.semantics(mergeDescendants = true) { this.contentDescription = description }
    } else Modifier
    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(p.cardTop, p.cardBottom)), shape)
            .border(
                1.dp,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.25f))),
                shape
            )
            .then(semanticsModifier)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun RexIconBadge(icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun RexProgress(progress: Float?, accent: Color, modifier: Modifier = Modifier) {
    val target = (progress ?: 0f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, label = "rexProgress")
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.18f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.6f), accent)))
        )
    }
}

/** Grafik mini dari riwayat pembacaan nyata. Tidak menggambar apa pun bila data < 2 titik. */
@Composable
fun RexSparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height * 0.9f - ((v - min) / range) * size.height * 0.8f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path, color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Kartu metrik generik untuk RAM / Storage / CPU / GPU. */
@Composable
fun RexMetricCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    primary: String?,
    secondary: String?,
    progress: Float?,
    progressLabel: String?,
    footers: List<String>,
    loading: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    val p = RexTheme.palette
    RexCard(modifier, accent, description) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RexIconBadge(icon, accent)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = p.textSecondary)
                when {
                    loading -> Text("Memuat…", style = MaterialTheme.typography.titleMedium, color = p.textSecondary)
                    primary == null -> Text(
                        REX_UNAVAILABLE,
                        style = MaterialTheme.typography.titleSmall,
                        color = p.textSecondary
                    )
                    else -> Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            primary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = p.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (secondary != null) {
                            Text(
                                " $secondary",
                                style = MaterialTheme.typography.bodyMedium,
                                color = p.textSecondary
                            )
                        }
                    }
                }
            }
        }
        if (!loading && progress != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RexProgress(progress, accent, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(progressLabel.orEmpty(), style = MaterialTheme.typography.labelMedium, color = p.textPrimary)
            }
        }
        if (!loading) {
            footers.forEach {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
            }
        }
    }
}

@Composable
fun RexLabelValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    val p = RexTheme.palette
    Row(
        modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: p.textPrimary
        )
    }
}
