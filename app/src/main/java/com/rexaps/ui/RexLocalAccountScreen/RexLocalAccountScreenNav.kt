package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SegTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = c.surfaceVariant.copy(.4f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(.4f))
    ) {
        Row(Modifier.padding(4.dp)) {
            labels.forEachIndexed { i, l ->
                val sel = i == selected
                val bg by animateColorAsState(if (sel) c.primary else Color.Transparent, label = "tabbg")
                val fg by animateColorAsState(if (sel) c.onPrimary else c.onSurfaceVariant, label = "tabfg")
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable { onSelect(i) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        l,
                        color = fg,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProfileTabs(ctl: RexProfileController, columns: Int, cellAspect: Float) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SegTabs(listOf("Video saya", "Story 24 jam"), ctl.tab) { ctl.tab = it }

        AnimatedContent(
            ctl.tab,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
            label = "tab"
        ) { t ->
            if (t == 0) {
                RexGrid(ctl.videos.size + 1, columns, cellAspect) { i, m ->
                    if (i == 0) {
                        AddTile(m, "Tambah", "mp4 • png • jpg") { ctl.onPickVideo() }
                    } else {
                        val f = ctl.videos[i - 1]
                        Box(m.pressable { ctl.feedIndex = i - 1 }.clip(RoundedCornerShape(16.dp))) {
                            MediaThumb(f, Modifier.fillMaxSize())
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(0f to Color.Transparent, .65f to Color.Transparent, 1f to Color.Black.copy(.55f))
                                    )
                                )
                            )
                            Icon(
                                if (f.extension.lowercase() == "mp4") Icons.Default.Movie else Icons.Default.Image,
                                null,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(16.dp)
                            )
                        }
                    }
                }
            } else {
                RexGrid(ctl.stories.size + 1, columns, cellAspect) { i, m ->
                    if (i == 0) AddTile(m, "Story baru", "teks • mp4") { ctl.showStoryCompose = true }
                    else StoryTile(ctl.stories[i - 1], ctl.acct, ctl.rev, m) { ctl.storyIndex = i - 1 }
                }
            }
        }

        Text(
            "Data: ${RexPaths.base.absolutePath}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(.7f)
        )
    }
}
