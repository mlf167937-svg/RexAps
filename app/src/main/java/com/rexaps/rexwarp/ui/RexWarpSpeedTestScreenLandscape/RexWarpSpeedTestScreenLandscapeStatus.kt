package com.rexaps.rexwarp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun MetricTile(modifier: Modifier, title: String, value: String, unit: String, accent: Color, icon: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(modifier.animateContentSize(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides accent) { icon() }
                }
                Text(title, style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                AnimatedContent(value, label = "metric-$title") { Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                Text(unit, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
