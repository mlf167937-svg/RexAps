// RexAccountScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Profil lokal portrait: cover ringkas, identitas, statistik, aksi, dan galeri video vertikal. */
@Composable
internal fun RexAccountScreenPortrait(ctl: RexProfileController) {
    val scroll = rememberScrollState()
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Aurora(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth()) {
                BannerView(
                    ctl.acct,
                    ctl.rev,
                    Modifier.fillMaxWidth().aspectRatio(BANNER_ASPECT)
                ) { scroll.value * 0.42f }

                ProfileEditButton(ctl, Modifier.align(Alignment.TopStart).padding(14.dp))

                StoryRingAvatar(
                    acct = ctl.acct,
                    rev = ctl.rev,
                    storyCount = ctl.stories.size,
                    dia = 96.dp,
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = 20.dp, y = 46.dp),
                    onClick = {
                        if (ctl.stories.isNotEmpty()) ctl.storyIndex = 0 else ctl.showStoryCompose = true
                    },
                    onAdd = { ctl.showStoryCompose = true }
                )
            }

            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 58.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Reveal(ctl.visible, 0) { ProfileIdentity(ctl.acct, centered = false) }
                Reveal(ctl.visible, 100) { ProfileStats(ctl) }
                Reveal(ctl.visible, 180) { ProfileActions(ctl) }
                Reveal(ctl.visible, 260) { ProfileTabs(ctl, columns = 3, cellAspect = 9f / 16f) }
            }
        }
    }
}
