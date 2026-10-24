// RexAccountScreenLandscape.kt
package com.rexaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Layout profil landscape: kolom kiri berisi banner + identitas + aksi,
 * kolom kanan berisi galeri 4 kolom yang bisa di-scroll.
 */
@Composable
internal fun RexAccountScreenLandscape(ctl: RexProfileController) {
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Aurora(Modifier.fillMaxSize())

        Row(Modifier.fillMaxSize()) {

            /* ── Kiri: profil ── */
            Column(
                Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(Modifier.fillMaxWidth()) {
                    BannerView(ctl.acct, ctl.rev, Modifier.fillMaxWidth().aspectRatio(BANNER_ASPECT)) { 0f }
                    StoryRingAvatar(
                        acct = ctl.acct,
                        rev = ctl.rev,
                        storyCount = ctl.stories.size,
                        dia = 104.dp,
                        modifier = Modifier.align(Alignment.BottomStart).offset(x = 20.dp, y = 52.dp),
                        onClick = {
                            if (ctl.stories.isNotEmpty()) ctl.storyIndex = 0 else ctl.showEdit = true
                        },
                        onAdd = { ctl.showStoryCompose = true }
                    )
                }

                Column(
                    Modifier.padding(horizontal = 20.dp).padding(top = 64.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Reveal(ctl.visible, 0) { ProfileIdentity(ctl.acct, centered = false) }
                    Reveal(ctl.visible, 120) { ProfileStats(ctl) }
                    Reveal(ctl.visible, 220) { ProfileActions(ctl) }
                }
            }

            /* ── Kanan: galeri ── */
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 20.dp, end = 20.dp, bottom = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Reveal(ctl.visible, 320) {
                    ProfileTabs(ctl, columns = 4, cellAspect = 3f / 4f)
                }
            }
        }
    }
}