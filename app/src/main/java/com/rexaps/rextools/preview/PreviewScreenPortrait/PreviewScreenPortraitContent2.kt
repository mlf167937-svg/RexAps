// PriviewScreen.kt
package com.rexaps.rextools.preview

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*
import kotlinx.coroutines.delay

@Composable
internal fun SingleView(
    item: PreviewItem,
    index: Int,
    total: Int,
    saving: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCopied: () -> Unit,
    onOpenFail: () -> Unit,
    onDownload: (String, MediaType) -> Unit
) {
    val ctx = LocalContext.current
    val s = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var copied by remember(item.url) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val progress by animateFloatAsState(
        if (total > 0) (index + 1f) / total else 0f,
        spring(stiffness = Spring.StiffnessMediumLow), label = "progress"
    )

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RexLg)
                .background(Color.Black)
                .border(hairline(0.5f), RexLg),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = index to item,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val fwd = targetState.first >= initialState.first
                    (slideInHorizontally(tween(400, easing = RexEase)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(200)))
                },
                label = "stage"
            ) { (i, current) ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (current.type) {
                        MediaType.IMAGE -> ZoomableImage(current.url, "Gambar ${i + 1} dari $total")
                        MediaType.VIDEO -> ExoVideoPlayer(current.url)
                        MediaType.UNKNOWN -> Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Text(
                                "Format tidak dikenali. Buka di browser untuk melihatnya.",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text(
                "${index + 1} / $total",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Surface(
            color = s.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = hairline(0.8f),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RexRoundButton(Icons.Default.ChevronLeft, "Sebelumnya", onPrev, size = 44.dp)
                    Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(s.surfaceContainerHighest)) {
                        Box(
                            Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(s.primary, s.tertiary)))
                        )
                    }
                    RexRoundButton(Icons.Default.ChevronRight, "Berikutnya", onNext, size = 44.dp)
                }

                Row(
                    Modifier.fillMaxWidth().clip(RexSm).background(s.surfaceContainerHigh).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = s.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(item.url))
                        copied = true
                        onCopied()
                    }) {
                        AnimatedContent(
                            targetState = copied,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.5f)) },
                            label = "copy"
                        ) { done ->
                            if (done) Icon(Icons.Default.Check, contentDescription = "Tersalin", tint = s.secondary, modifier = Modifier.size(20.dp))
                            else Icon(Icons.Default.ContentCopy, contentDescription = "Salin link", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RexOutlinedButton(
                        "Buka", Icons.Default.OpenInNew,
                        {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                                .onFailure { onOpenFail() }
                        },
                        Modifier.weight(1f)
                    )
                    RexPrimaryButton(
                        text = "Simpan",
                        icon = Icons.Default.Download,
                        loading = saving,
                        loadingText = "Menyimpan…",
                        onClick = { onDownload(item.url, item.type) },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }
}
