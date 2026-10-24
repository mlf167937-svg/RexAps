package com.rexaps.rextools.tiktok

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*

@Composable
internal fun FullProfileCard(r: com.rexaps.rextools.TiktokFullResult, onOpen: (String) -> Unit) {
    val stories = remember(r) { r.stories.entries.toList() }
    val videos = remember(r) { r.videos.entries.toList() }
    var tab by rememberSaveable { mutableIntStateOf(if (stories.isEmpty() && videos.isNotEmpty()) 1 else 0) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    RexPanel(padding = 0.dp, spacing = 0.dp, glow = true) {
        ProfileHeader(
            avatar = r.avatar, name = r.name, username = r.username, bio = r.bio,
            verified = false, region = null
        )
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stories.isEmpty() && videos.isEmpty()) {
                RexEmptyState(Icons.Default.Videocam, "Tidak ada konten", "Story atau video publik tidak ditemukan.")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RexFilterChip("Story (${stories.size})", tab == 0) { tab = 0; expanded = false }
                    RexFilterChip("Video (${videos.size})", tab == 1) { tab = 1; expanded = false }
                }

                val items = if (tab == 0) stories else videos
                val shown = if (expanded) items else items.take(PREVIEW_COUNT)

                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(300, delayMillis = 80)) + slideInVertically(tween(420, easing = RexEase)) { it / 12 }) togetherWith
                            fadeOut(tween(140))
                    },
                    label = "tab"
                ) { t ->
                    Column(Modifier.animateContentSize(spring(0.85f)), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (t == 0) {
                            shown.forEachIndexed { i, e ->
                                StoryRow(e.key.toString(), i, { onOpen(e.value.toString()) })
                            }
                        } else {
                            shown.chunked(2).forEachIndexed { row, pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEachIndexed { c, e ->
                                        VideoTile(
                                            label = e.key.toString(),
                                            index = row * 2 + c,
                                            onClick = { onOpen(e.value.toString()) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (items.size > PREVIEW_COUNT) {
                    RexOutlinedButton(
                        text = if (expanded) "Ciutkan" else "Tampilkan semua (${items.size})",
                        icon = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            r.username?.takeIf { it.isNotBlank() }?.let { u ->
                RexTonalButton(
                    "Buka profil di TikTok", Icons.Default.OpenInNew,
                    { onOpen("https://www.tiktok.com/@$u") }, Modifier.fillMaxWidth()
                )
            }
        }
    }
}
