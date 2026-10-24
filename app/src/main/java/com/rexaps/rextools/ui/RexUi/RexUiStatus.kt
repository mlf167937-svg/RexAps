// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

@Composable
fun RexPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    content: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}

/** Angka murni dihitung naik (count-up); teks seperti "1.2M" tampil apa adanya. */
@Composable
fun RexStat(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, index: Int = 0) {
    val s = MaterialTheme.colorScheme
    val numeric = remember(value) { if (value.isNotEmpty() && value.all { it.isDigit() }) value.toLongOrNull() else null }
    val fraction = remember(value) { Animatable(0f) }
    LaunchedEffect(value) {
        if (numeric != null) fraction.animateTo(1f, tween(1100, delayMillis = index * 80, easing = RexEase))
    }
    val shown = when {
        numeric == null -> value
        fraction.value >= 1f -> NumberFormat.getInstance().format(numeric)
        else -> NumberFormat.getInstance().format((numeric * fraction.value.toDouble()).toLong())
    }
    Column(
        modifier
            .entrance(index, 60, 14.dp)
            .clip(RexMd)
            .background(Brush.verticalGradient(listOf(s.surfaceContainerHigh.copy(alpha = 0.85f), s.surfaceContainer.copy(alpha = 0.6f))))
            .border(1.dp, s.outlineVariant.copy(alpha = 0.5f), RexMd)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(18.dp))
        Text(shown, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall, color = s.onSurfaceVariant, maxLines = 1)
    }
}

/* ═════════════════════ FEEDBACK ═════════════════════ */

@Stable
class RexToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    var tick by mutableStateOf(0)
        private set

    fun show(text: String, error: Boolean = false) {
        message = text
        isError = error
        tick++
    }

    fun dismiss() { message = null }
}
