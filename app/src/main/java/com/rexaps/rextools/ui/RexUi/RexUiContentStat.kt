package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.text.NumberFormat

/** Angka murni dihitung naik (count-up); teks seperti "1.2M" tampil apa adanya. */
@Composable
fun RexStat(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, index: Int = 0) {
    val s = MaterialTheme.colorScheme
    val numeric = remember(value) { if (value.isNotEmpty() && value.all { it.isDigit() }) value.toLongOrNull() else null }
    val fraction = remember(value) { Animatable(0f) }
    LaunchedEffect(value) {
        if (numeric != null) fraction.animateTo(1f, tween(1100, delayMillis = index * 80, easing = RexEase))
    }
    val shown = when {
        numeric == null -> value
        fraction.value >= 1f -> NumberFormat.getInstance().format(numeric)
        else -> NumberFormat.getInstance().format((numeric * fraction.value.toDouble()).toLong())
    }
    Column(
        modifier
            .entrance(index, 60, 14.dp)
            .clip(RexMd)
            .background(Brush.verticalGradient(listOf(s.surfaceContainerHigh.copy(alpha = 0.85f), s.surfaceContainer.copy(alpha = 0.6f))))
            .border(1.dp, s.outlineVariant.copy(alpha = 0.5f), RexMd)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(18.dp))
        Text(shown, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall, color = s.onSurfaceVariant, maxLines = 1)
    }
}

/* ═════════════════════ FEEDBACK ═════════════════════ */

@Composable
fun RexErrorSlot(error: String?, title: String) {
    var last by remember { mutableStateOf("") }
    SideEffect { if (error != null) last = error }
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(spring(0.8f)),
        exit = fadeOut() + shrinkVertically()
    ) {
        RexErrorBanner(title, error ?: last)
    }
}

@Composable
fun RexErrorBanner(title: String, message: String) {
    val s = MaterialTheme.colorScheme
    val shake = remember { Animatable(0f) }
    LaunchedEffect(message) {
        for (v in listOf(-12f, 10f, -7f, 4f, 0f)) shake.animateTo(v, tween(55))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shake.value }
            .clip(RexMd)
            .background(s.errorContainer)
            .border(1.dp, s.error.copy(alpha = 0.35f), RexMd)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = s.onErrorContainer)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = s.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = s.onErrorContainer)
        }
    }
}

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
fun RexSkeletonPanel(media: Boolean = true, modifier: Modifier = Modifier) {
    RexPanel(modifier.shimmer()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexSkeleton(Modifier.size(46.dp), CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RexSkeleton(Modifier.width(140.dp).height(13.dp))
                RexSkeleton(Modifier.width(90.dp).height(11.dp))
            }
        }
        RexSkeleton(Modifier.fillMaxWidth().height(13.dp))
        if (media) RexSkeleton(Modifier.fillMaxWidth().height(190.dp), RexMd)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RexSkeleton(Modifier.weight(1f).height(50.dp), RexMd)
            RexSkeleton(Modifier.weight(1f).height(50.dp), RexMd)
        }
    }
}

/* ═════════════════════ MEDIA ═════════════════════ */

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

/** Avatar dengan cincin gradien yang berputar pelan. */
@Composable
fun RexAvatar(
    model: Any?,
    contentDescription: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Dp = 3.dp
) {
    val s = MaterialTheme.colorScheme
    val angle = rememberInfiniteTransition(label = "ring")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "angle")
    val brush = remember(s.primary, s.secondary, s.tertiary) {
        Brush.sweepGradient(listOf(s.primary, s.secondary, s.tertiary, s.primary))
    }
    Box(
        modifier
            .size(size)
            .background(s.surface, CircleShape)
            .drawBehind {
                val w = ring.toPx()
                rotate(angle.value) {
                    drawCircle(brush, radius = this.size.minDimension / 2f - w / 2f, style = Stroke(w))
                }
            }
    ) {
        RexImage(
            model, contentDescription,
            Modifier.fillMaxSize().padding(ring + 4.dp).clip(CircleShape).background(s.surfaceVariant),
            ContentScale.Crop
        )
    }
}
