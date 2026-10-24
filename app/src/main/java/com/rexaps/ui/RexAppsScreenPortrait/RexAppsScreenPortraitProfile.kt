// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ProPortraitProfile(appCount: Int, themeLabel: String, isActive: Boolean) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 130.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProGlass(Modifier.fillMaxWidth().entrance(0), radius = 32.dp) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SpinningAvatar(isActive = isActive, size = 124.dp)
                Text(
                    "RexAps Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Kelola aplikasi dan preferensimu",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(label = "Aplikasi", value = "$appCount", modifier = Modifier.weight(1f))
                    StatCard(label = "Tema", value = themeLabel, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Column(Modifier.fillMaxWidth().entrance(1)) {
            SectionHeader(
                title = "Ikuti & Dukung Rex",
                subtitle = "Temukan aku di platform favoritmu"
            )
        }

        Spacer(Modifier.height(14.dp))

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            socialLinks.forEachIndexed { index, link ->
                SocialLinkCard(link = link, modifier = Modifier.entrance(2 + index))
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Dibuat dengan ❤ oleh Rex",
            style = MaterialTheme.typography.labelSmall,
            color = c.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
