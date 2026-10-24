// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun ProPortraitNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = c.surfaceVariant.copy(alpha = 0.9f),
            shadowElevation = 20.dp,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, c.outline.copy(alpha = 0.25f))
        ) {
            Row(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomTabs.forEachIndexed { index, tab ->
                    ProNavItem(tab = tab, selected = index == selected, onClick = { onSelect(index) })
                }
            }
        }
    }
}

@Composable
internal fun ProNavItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) c.primary else Color.Transparent,
        animationSpec = tween(300),
        label = "pnBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) c.onPrimary else c.onSurfaceVariant,
        animationSpec = tween(300),
        label = "pnFg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pnScale"
    )

    Row(
        Modifier
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 16.dp else 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = fg,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
        )
        if (selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1
            )
        }
    }
}

/* ═════════════════════ Home portrait ═════════════════════ */
