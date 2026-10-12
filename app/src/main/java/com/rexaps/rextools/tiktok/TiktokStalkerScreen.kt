// TiktokStalkerScreen.kt
package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rexaps.rextools.ui.*

@Composable
fun TiktokStalkerScreen(
    onBack: () -> Unit = {},
    vm: TiktokStalkerViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current

    RexScreen(title = "TikTok Stalker", onBack = onBack) {
        RexHero(
            icon = Icons.Default.Person,
            title = "Cari profil TikTok",
            description = "Masukkan username untuk melihat info profil publik."
        )

        RexField(
            value = state.username,
            onChange = vm::onUsernameChange,
            label = "Username (tanpa @)",
            placeholder = "contoh: MrBeast",
            icon = Icons.Default.Search,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                if (!state.loading) vm.stalk()
            })
        )

        RexPrimaryButton(
            text = "Stalk",
            icon = Icons.Default.Search,
            loading = state.loading,
            loadingText = "Mencari…",
            onClick = {
                focusManager.clearFocus()
                vm.stalk()
            }
        )

        RexErrorSlot(state.error, "Terjadi kesalahan")

        state.result?.let { r ->
            ProfileCard(r) {
                r.link?.let { link -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
            }
        }

        if (state.result == null && state.error == null && !state.loading) {
            RexEmptyState(
                icon = Icons.Default.Search,
                title = "Belum ada hasil",
                message = "Ketik username lalu tekan Stalk untuk menampilkan profil."
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileCard(r: com.rexaps.rextools.TiktokStalkResult, onOpen: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val ring = Brush.sweepGradient(listOf(s.primary, s.tertiary, s.primary))

    RexPanel(padding = 22.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = r.avatar,
                contentDescription = r.name?.let { "Foto profil $it" } ?: "Foto profil",
                modifier = Modifier
                    .size(104.dp)
                    .border(2.dp, ring, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(s.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        r.name ?: "-",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false).semantics { heading() }
                    )
                    if (r.verified?.equals("Verified", ignoreCase = true) == true) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = s.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text("@${r.username ?: "-"}", style = MaterialTheme.typography.bodyMedium, color = s.onSurfaceVariant)
            }

            r.bio?.takeIf { it.isNotBlank() && it != "-" }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }

            r.region?.takeIf { it.isNotBlank() && it != "-" }?.let { RexPill("Region: $it") }
        }

        r.stats?.let { st ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RexStat("Followers", st.followers ?: "-", Modifier.weight(1f))
                    RexStat("Following", st.following ?: "-", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RexStat("Likes", st.likes ?: "-", Modifier.weight(1f))
                    RexStat("Videos", st.videos ?: "-", Modifier.weight(1f))
                }
            }
        }

        r.link?.takeIf { it.isNotBlank() }?.let {
            RexTonalButton("Buka di TikTok", Icons.Default.OpenInNew, onOpen, Modifier.fillMaxWidth())
        }
    }
}