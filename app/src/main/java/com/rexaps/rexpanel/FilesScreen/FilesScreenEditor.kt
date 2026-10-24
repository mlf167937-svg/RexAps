package com.rexaps.rexpanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun Breadcrumbs(b: FileBrowserState) {
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val segs = remember(b.path) {
        buildList {
            add("/" to "/")
            var acc = ""
            b.path.split('/').filter { it.isNotEmpty() }.forEach { acc += "/$it"; add(it to acc) }
        }
    }
    LaunchedEffect(segs) {
        withFrameNanos { }
        scroll.animateScrollTo(scroll.maxValue)
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segs.forEachIndexed { i, (label, target) ->
            val last = i == segs.lastIndex
            if (i > 0) Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, enabled = !last) { b.go(target) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (last) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
