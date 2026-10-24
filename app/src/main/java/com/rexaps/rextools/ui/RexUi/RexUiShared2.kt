// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun RexToastHost(state: RexToastState, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    LaunchedEffect(state.tick) {
        if (state.message != null) {
            delay(2600)
            state.dismiss()
        }
    }
    var last by remember { mutableStateOf("") }
    SideEffect { state.message?.let { last = it } }
    val accent = if (state.isError) s.error else s.secondary

    AnimatedVisibility(
        visible = state.message != null,
        modifier = modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        enter = slideInVertically(spring(0.7f, Spring.StiffnessMediumLow)) { it * 2 } + fadeIn() + scaleIn(initialScale = 0.85f),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)) + scaleOut(targetScale = 0.9f)
    ) {
        Row(
            Modifier
                .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
                .clip(CircleShape)
                .background(s.surfaceContainerHighest)
                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (state.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)
            )
            Text(state.message ?: last, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 2)
        }
    }
}

/* ═════════════════════ LOADING / PHASE ═════════════════════ */

enum class RexPhase { Idle, Loading, Result, Hidden }

/** Pindah mulus antara kondisi kosong, loading (skeleton), dan hasil. */
@Composable
fun RexPhaseSwitch(
    phase: RexPhase,
    modifier: Modifier = Modifier,
    idle: @Composable () -> Unit,
    loading: @Composable () -> Unit,
    result: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = phase,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (fadeIn(tween(340, delayMillis = 100)) +
                slideInVertically(tween(480, easing = RexEase)) { it / 14 } +
                scaleIn(tween(340, delayMillis = 100), initialScale = 0.97f))
                .togetherWith(fadeOut(tween(160)))
                .using(SizeTransform(clip = false))
        },
        label = "phase"
    ) { p ->
        when (p) {
            RexPhase.Idle -> idle()
            RexPhase.Loading -> loading()
            RexPhase.Result -> result()
            RexPhase.Hidden -> Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
fun RexSkeleton(modifier: Modifier = Modifier, shape: Shape = RexSm) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
}

@Composable
fun RexImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val s = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val request = remember(model) { ImageRequest.Builder(ctx).data(model).crossfade(350).build() }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize().defaultMinSize(minHeight = 160.dp).background(s.surfaceContainerHigh).shimmer())
        },
        error = {
            Box(
                Modifier.fillMaxSize().defaultMinSize(minHeight = 120.dp).background(s.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BrokenImage, contentDescription = "Gagal memuat", tint = s.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun RexEqualizer(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = listOf(520, 700, 430, 610).mapIndexed { i, ms ->
        t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i")
    }
    Row(modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        bars.forEach { f ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .graphicsLayer { scaleY = f.value; transformOrigin = TransformOrigin(0.5f, 1f) }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
