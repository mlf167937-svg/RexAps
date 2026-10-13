// PinterestScreen.kt
package com.rexaps.rextools.search

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.downloader.ImageDownloader
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PinterestScreen(
    onBack: () -> Unit = {},
    vm: PinterestViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val toast = rememberRexToast()
    var query by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val phase = when {
        state.loading -> RexPhase.Loading
        state.images.isNotEmpty() -> RexPhase.Result
        state.error != null -> RexPhase.Hidden
        else -> RexPhase.Idle
    }

    RexScreen(title = "Pinterest", onBack = onBack, scroll = false, toast = toast) {
        RexField(
            value = query,
            onChange = { query = it },
            label = "Cari gambar Pinterest",
            icon = Icons.Default.Search,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focus.clearFocus()
                if (!state.loading) vm.search(query)
            }),
            modifier = Modifier.entrance(0)
        )

        RexPrimaryButton(
            text = "Cari",
            icon = Icons.Default.Search,
            loading = state.loading,
            loadingText = "Mencari…",
            modifier = Modifier.entrance(1),
            onClick = {
                focus.clearFocus()
                vm.search(query)
            }
        )

        RexErrorSlot(state.error, "Gagal memuat gambar")

        RexPhaseSwitch(
            phase = phase,
            modifier = Modifier.weight(1f),
            idle = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RexEmptyState(
                        Icons.Default.Image, "Belum ada gambar",
                        "Ketik kata kunci lalu tekan Cari, hasilnya muncul di sini."
                    )
                }
            },
            loading = {
                Box(
                    Modifier.fillMaxSize().padding(bottom = 8.dp)
                        .clip(RexLg)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .shimmer()
                )
            },
            result = {
                if (state.images.isNotEmpty()) {
                    Deck(
                        images = state.images,
                        index = state.currentIndex,
                        saving = saving,
                        onPrev = { vm.prev() },
                        onNext = { vm.next() },
                        onSave = { url ->
                            if (!saving) {
                                saving = true
                                scope.launch {
                                    ImageDownloader.download(context, url)
                                        .onSuccess { toast.show("Gambar tersimpan") }
                                        .onFailure { toast.show("Gagal: ${it.message}", true) }
                                    saving = false
                                }
                            }
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun Deck(
    images: List<String>,
    index: Int,
    saving: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSave: (String) -> Unit
) {
    val s = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val prevCb by rememberUpdatedState(onPrev)
    val nextCb by rememberUpdatedState(onNext)

    val total = images.size
    val safeIndex = index.coerceIn(0, images.lastIndex)
    val currentUrl = images[safeIndex]
    val progress by animateFloatAsState((safeIndex + 1f) / total, spring(stiffness = Spring.StiffnessMediumLow), label = "progress")

    Column(Modifier.fillMaxSize().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 10.dp)) {
            // Kartu bayangan di belakang
            listOf(0.90f to 0.35f, 0.95f to 0.6f).forEachIndexed { i, (sc, a) ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = sc
                            scaleY = sc
                            translationY = (2 - i) * 14f
                            alpha = a
                        }
                        .clip(RexLg)
                        .background(s.surfaceContainerHigh)
                        .border(hairline(), RexLg)
                )
            }

            // Kartu utama
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = (offsetX.value / 60f).coerceIn(-12f, 12f)
                        alpha = 1f - (abs(offsetX.value) / (size.width * 1.4f)).coerceIn(0f, 0.6f)
                    }
                    .shadow(22.dp, RexLg, ambientColor = s.primary.copy(alpha = 0.4f), spotColor = s.primary.copy(alpha = 0.4f))
                    .clip(RexLg)
                    .border(hairline(), RexLg)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { scope.launch { offsetX.stop() } },
                            onDragEnd = {
                                scope.launch {
                                    val x = offsetX.value
                                    val w = size.width.toFloat()
                                    if (abs(x) > w * 0.28f) {
                                        val dir = if (x > 0) 1 else -1
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        offsetX.animateTo(dir * w * 1.15f, tween(200, easing = FastOutLinearInEasing))
                                        if (dir > 0) prevCb() else nextCb()
                                        withFrameNanos { }
                                        offsetX.snapTo(-dir * w * 0.3f)
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
                                    } else {
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            },
                            onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                scope.launch { offsetX.snapTo(offsetX.value + amount * 0.9f) }
                            }
                        )
                    }
            ) {
                RexImage(currentUrl, "Gambar ${safeIndex + 1} dari $total", Modifier.fillMaxSize(), ContentScale.Crop)
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(110.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                ) {
                    Text(
                        "${safeIndex + 1} / $total",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Progress tipis
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(s.surfaceContainerHighest)) {
            Box(
                Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(s.primary, s.tertiary)))
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RexRoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Gambar sebelumnya", { onPrev() })
            RexPrimaryButton(
                text = "Download",
                icon = Icons.Default.Download,
                loading = saving,
                loadingText = "Menyimpan…",
                modifier = Modifier.weight(1f),
                onClick = { onSave(currentUrl) }
            )
            RexRoundButton(Icons.AutoMirrored.Outlined.ArrowForward, "Gambar berikutnya", { onNext() })
        }
    }
}