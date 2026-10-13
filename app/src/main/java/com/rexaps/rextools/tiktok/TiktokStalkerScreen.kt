// TiktokStalkerScreen.kt
package com.rexaps.rextools.tiktok

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.rexaps.rextools.ui.*

@Composable
fun TiktokStalkerScreen(
    onBack: () -> Unit = {},
    vm: TiktokStalkerViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val toast = rememberRexToast()

    val phase = when {
        state.loading -> RexPhase.Loading
        state.result != null -> RexPhase.Result
        state.error != null -> RexPhase.Hidden
        else -> RexPhase.Idle
    }

    RexScreen(title = "TikTok Stalker", onBack = onBack, toast = toast) {
        RexHero(
            icon = Icons.Default.Person,
            title = "Cari profil TikTok",
            description = "Masukkan username untuk melihat info profil publik.",
            modifier = Modifier.entrance(0)
        )

        Column(Modifier.entrance(1), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RexField(
                value = state.username,
                onChange = vm::onUsernameChange,
                label = "Username (tanpa @)",
                placeholder = "contoh: MrBeast",
                icon = Icons.Default.Search,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focus.clearFocus()
                    if (!state.loading) vm.stalk()
                })
            )
            RexQuickAction("Tempel", Icons.Default.ContentPaste, {
                clipboard.getText()?.text?.trim()?.removePrefix("@")
                    ?.takeIf { it.isNotEmpty() }?.let(vm::onUsernameChange)
            })
        }

        RexPrimaryButton(
            text = "Stalk",
            icon = Icons.Default.Search,
            loading = state.loading,
            loadingText = "Mencari…",
            modifier = Modifier.entrance(2),
            onClick = {
                focus.clearFocus()
                vm.stalk()
            }
        )

        RexErrorSlot(state.error, "Terjadi kesalahan")

        RexPhaseSwitch(
            phase = phase,
            idle = {
                RexEmptyState(
                    Icons.Default.Search, "Belum ada hasil",
                    "Ketik username lalu tekan Stalk untuk menampilkan profil."
                )
            },
            loading = { RexSkeletonPanel(media = false) },
            result = {
                state.result?.let { r ->
                    ProfileCard(r) {
                        r.link?.let { link ->
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                                .onFailure { toast.show("Tidak bisa membuka link", true) }
                        }
                    }
                }
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileCard(r: com.rexaps.rextools.TiktokStalkResult, onOpen: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val badge = rememberInfiniteTransition(label = "badge")
        .animateFloat(1f, 1.18f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")

    RexPanel(padding = 0.dp, spacing = 0.dp, glow = true) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().height(104.dp).background(
                    Brush.linearGradient(
                        listOf(s.primary.copy(alpha = 0.40f), s.tertiary.copy(alpha = 0.28f), s.secondary.copy(alpha = 0.22f))
                    )
                )
            )
            Column(
                Modifier.fillMaxWidth().padding(top = 52.dp).padding(horizontal = 20.dp).padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RexAvatar(
                    model = r.avatar,
                    contentDescription = r.name?.let { "Foto profil $it" } ?: "Foto profil",
                    size = 108.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            r.name ?: "-",
                            style = MaterialTheme.typography.titleLarge,
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
                                modifier = Modifier.size(20.dp).graphicsLayer { scaleX = badge.value; scaleY = badge.value }
                            )
                        }
                    }
                    Text("@${r.username ?: "-"}", style = MaterialTheme.typography.bodyMedium, color = s.onSurfaceVariant)
                }

                r.bio?.takeIf { it.isNotBlank() && it != "-" }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
                r.region?.takeIf { it.isNotBlank() && it != "-" }?.let { RexPill("Region: $it") }

                r.stats?.let { st ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RexStat("Followers", st.followers ?: "-", Modifier.weight(1f), index = 0)
                            RexStat("Following", st.following ?: "-", Modifier.weight(1f), index = 1)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RexStat("Likes", st.likes ?: "-", Modifier.weight(1f), index = 2)
                            RexStat("Videos", st.videos ?: "-", Modifier.weight(1f), index = 3)
                        }
                    }
                }

                r.link?.takeIf { it.isNotBlank() }?.let {
                    RexTonalButton("Buka di TikTok", Icons.Default.OpenInNew, onOpen, Modifier.fillMaxWidth())
                }
            }
        }
    }
}