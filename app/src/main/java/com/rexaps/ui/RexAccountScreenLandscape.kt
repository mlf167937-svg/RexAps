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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Layout landscape memakai panel profil ringkas dan galeri yang mendapat ruang utama. */
@Composable
internal fun RexAccountScreenLandscape(ctl: RexProfileController) {
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Aurora(Modifier.fillMaxSize())

        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(320.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(Modifier.fillMaxWidth()) {
                    BannerView(ctl.acct, ctl.rev, Modifier.fillMaxWidth().aspectRatio(BANNER_ASPECT)) { 0f }
                    ProfileEditButton(ctl, Modifier.align(Alignment.TopStart).padding(14.dp))
                    StoryRingAvatar(
                        acct = ctl.acct,
                        rev = ctl.rev,
                        storyCount = ctl.stories.size,
                        dia = 88.dp,
                        modifier = Modifier.align(Alignment.BottomStart).offset(x = 18.dp, y = 42.dp),
                        onClick = {
                            if (ctl.stories.isNotEmpty()) ctl.storyIndex = 0 else ctl.showStoryCompose = true
                        },
                        onAdd = { ctl.showStoryCompose = true }
                    )
                }

                Column(
                    Modifier.padding(horizontal = 18.dp).padding(top = 54.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Reveal(ctl.visible, 0) { ProfileIdentity(ctl.acct, centered = false) }
                    Reveal(ctl.visible, 100) { ProfileStats(ctl) }
                    Reveal(ctl.visible, 180) { ProfileActions(ctl) }
                }
            }

            Column(
                Modifier.weight(1f)
                    .fillMaxHeight()
                    .padding(start = 10.dp, top = 16.dp, end = 18.dp, bottom = 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Reveal(ctl.visible, 240) {
                    ProfileTabs(ctl, columns = 4, cellAspect = 3f / 4f)
                }
            }
        }
    }
}
