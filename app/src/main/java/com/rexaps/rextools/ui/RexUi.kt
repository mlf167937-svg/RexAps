// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import java.text.NumberFormat

/* ═════════════════════ TOKENS ═════════════════════ */

val RexSm = RoundedCornerShape(12.dp)
val RexMd = RoundedCornerShape(18.dp)
val RexLg = RoundedCornerShape(26.dp)

/** Easing "ease-out expo": cepat di awal, mendarat sangat halus. */
val RexEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun hairline(alpha: Float = 0.7f) =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))

/* ═════════════════════ THEME (opsional) ═════════════════════ */

private val RexType = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        labelSmall = t.labelSmall.copy(letterSpacing = 0.8.sp)
    )
}

@Composable
fun RexTheme(content: @Composable () -> Unit) {
    // Keep RexTools inside the palette selected by the RexAps shell.
    MaterialTheme(typography = RexType, content = content)
}

/* ═════════════════════ MODIFIERS ═════════════════════ */

/** Skala menyusut halus saat ditekan (pegas). */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    graphicsLayer { scaleX = sc; scaleY = sc }
}

/** Muncul berurutan: fade + naik + sedikit membesar. */
fun Modifier.entrance(index: Int = 0, delayPer: Int = 70, offset: Dp = 22.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { offset.toPx() }
    LaunchedEffect(Unit) {
        delay((index * delayPer).toLong())
        progress.animateTo(1f, tween(560, easing = RexEase))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * px
        val sc = 0.97f + 0.03f * p
        scaleX = sc
        scaleY = sc
    }
}

/** Kilau bergerak untuk placeholder loading. */
fun Modifier.shimmer(): Modifier = composed {
    val s = MaterialTheme.colorScheme
    val x = rememberInfiniteTransition(label = "shimmer")
        .animateFloat(-0.5f, 1.5f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "x")
    drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, s.onSurface.copy(alpha = 0.09f), Color.Transparent),
                start = Offset(w * x.value - w * 0.35f, 0f),
                end = Offset(w * x.value, size.height * 0.6f)
            )
        )
    }
}

/* ═════════════════════ BACKGROUND & SCREEN ═════════════════════ */

/** Latar: dua cahaya aksen yang melayang pelan + grid tipis. Animasi dibaca di fase draw (tanpa recomposition). */
@Composable
fun RexBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val s = MaterialTheme.colorScheme
    val p = rememberInfiniteTransition(label = "aurora")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val grid = s.onSurface.copy(alpha = 0.03f)
    Box(
        modifier
            .fillMaxSize()
            .background(s.background)
            .drawBehind {
                val w = size.width
                val h = size.height
                val v = p.value
                drawRect(
                    Brush.radialGradient(
                        listOf(s.primary.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(w * (0.95f - 0.35f * v), h * (0.02f + 0.10f * v)),
                        radius = w * 1.05f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(s.tertiary.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w * (0.05f + 0.30f * v), h * (0.82f - 0.12f * v)),
                        radius = w * 0.95f
                    )
                )
                val step = 36.dp.toPx()
                var gx = 0f
                while (gx < w) { drawLine(grid, Offset(gx, 0f), Offset(gx, h), 1f); gx += step }
                var gy = 0f
                while (gy < h) { drawLine(grid, Offset(0f, gy), Offset(w, gy), 1f); gy += step }
            },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexScreen(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    scroll: Boolean = true,
    padded: Boolean = true,
    toast: RexToastState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scroll && scrollState.value > 8 } }
    val barColor by animateColorAsState(
        if (scrolled) s.background.copy(alpha = 0.88f) else Color.Transparent, tween(250), label = "bar"
    )
    val lineAlpha by animateFloatAsState(if (scrolled) 0.7f else 0f, tween(250), label = "line")

    RexBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            AnimatedContent(
                                targetState = title to subtitle,
                                transitionSpec = {
                                    (fadeIn(tween(240, delayMillis = 80)) + slideInVertically(tween(300, easing = RexEase)) { it / 3 }) togetherWith
                                        fadeOut(tween(120))
                                },
                                label = "title"
                            ) { (t, sub) ->
                                Column {
                                    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (sub != null) {
                                        Text(sub, style = MaterialTheme.typography.labelMedium, color = s.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        navigationIcon = { RexBackButton(onBack) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barColor,
                            scrolledContainerColor = barColor
                        )
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(s.outlineVariant.copy(alpha = lineAlpha)))
                }
            }
        ) { pad ->
            var m = Modifier.fillMaxSize().padding(pad)
            if (scroll) m = m.verticalScroll(scrollState)
            m = m.entrance(0, offset = 16.dp)
            if (padded) m = m.padding(horizontal = 20.dp, vertical = 8.dp)
            Column(
                modifier = m,
                verticalArrangement = if (padded) Arrangement.spacedBy(16.dp) else Arrangement.Top,
                content = content
            )
        }
        if (toast != null) RexToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun RexBackButton(onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(start = 12.dp).size(40.dp).pressScale(source, 0.88f),
        shape = CircleShape,
        color = s.surfaceContainerHigh.copy(alpha = 0.8f),
        border = hairline(0.9f),
        interactionSource = source
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali", modifier = Modifier.size(20.dp))
        }
    }
}

/* ═════════════════════ PANEL ═════════════════════ */

@Composable
fun RexPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RexLg,
    padding: Dp = 16.dp,
    spacing: Dp = 16.dp,
    glow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = MaterialTheme.colorScheme
    val border = BorderStroke(
        1.dp,
        Brush.linearGradient(
            listOf(s.outline.copy(alpha = 0.55f), s.outlineVariant.copy(alpha = 0.25f), s.primary.copy(alpha = 0.20f))
        )
    )
    val color = s.surfaceContainerLow.copy(alpha = 0.80f)
    val glowMod = if (glow) {
        Modifier.shadow(26.dp, shape, ambientColor = s.primary.copy(alpha = 0.35f), spotColor = s.primary.copy(alpha = 0.35f))
    } else Modifier
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(padding), Arrangement.spacedBy(spacing), content = content)
    }
    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth().then(glowMod),
            shape = shape, color = color, border = border, content = body
        )
    } else {
        val src = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().pressScale(src, 0.97f).then(glowMod),
            shape = shape, color = color, border = border,
            interactionSource = src, content = body
        )
    }
}

/* ═════════════════════ ICON TILE & HERO ═════════════════════ */

@Composable
fun RexIconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 48.dp, pulse: Boolean = false) {
    val s = MaterialTheme.colorScheme
    val glow = rememberInfiniteTransition(label = "tile")
        .animateFloat(0.15f, 0.5f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    Box(
        modifier
            .size(size)
            .drawBehind {
                if (pulse) {
                    drawCircle(
                        Brush.radialGradient(listOf(s.primary.copy(alpha = glow.value), Color.Transparent)),
                        radius = this.size.minDimension * 0.95f
                    )
                }
            }
            .clip(RexSm)
            .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.24f), s.tertiary.copy(alpha = 0.14f))))
            .border(1.dp, Brush.linearGradient(listOf(s.primary.copy(alpha = 0.6f), s.tertiary.copy(alpha = 0.15f))), RexSm),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun RexHero(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RexIconTile(icon, size = 54.dp, pulse = true)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RexOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/* ═════════════════════ INPUT ═════════════════════ */

@Composable
fun RexField(
    value: String,
    onChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    icon: ImageVector? = null,
    clearable: Boolean = true,
    shape: Shape = RexMd,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val glow by animateDpAsState(if (focused) 16.dp else 0.dp, tween(320), label = "glow")
    val trailing: (@Composable () -> Unit)? = if (clearable) {
        {
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f)
            ) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus teks")
                }
            }
        }
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailing,
        singleLine = true,
        shape = shape,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = source,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = s.surfaceContainerLow,
            unfocusedContainerColor = s.surfaceContainerLow,
            focusedBorderColor = s.primary,
            unfocusedBorderColor = s.outlineVariant,
            focusedLeadingIconColor = s.primary,
            unfocusedLeadingIconColor = s.onSurfaceVariant,
            focusedLabelColor = s.primary,
            cursorColor = s.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .shadow(glow, shape, ambientColor = s.primary, spotColor = s.primary)
    )
}

/* ═════════════════════ BUTTONS ═════════════════════ */

@Composable
fun RexPrimaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    loadingText: String = "Memproses…",
    enabled: Boolean = true
) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val canClick = enabled && !loading
    val glow by animateDpAsState(if (enabled) 18.dp else 0.dp, tween(300), label = "glow")
    val fg by animateColorAsState(if (enabled) s.onPrimary else s.onSurfaceVariant, label = "fg")
    val sweep = rememberInfiniteTransition(label = "sweep")
        .animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "x")
    val brush = Brush.linearGradient(listOf(s.primary, s.tertiary))

    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .pressScale(source, 0.97f)
            .shadow(glow, RexMd, ambientColor = s.primary, spotColor = s.primary)
            .clip(RexMd)
            .then(if (enabled) Modifier.background(brush) else Modifier.background(s.surfaceContainerHighest))
            .drawWithContent {
                drawContent()
                if (canClick) {
                    val w = size.width
                    val x = w * sweep.value
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                            start = Offset(x - w * 0.25f, 0f),
                            end = Offset(x, size.height)
                        )
                    )
                }
            }
            .clickable(interactionSource = source, indication = null, enabled = canClick, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f)) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.8f))
            },
            label = "btn"
        ) { isLoading ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = fg)
                    Spacer(Modifier.width(10.dp))
                    Text(loadingText, color = fg, fontWeight = FontWeight.SemiBold)
                } else {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RexButtonBase(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier,
    container: Color,
    content: Color,
    border: BorderStroke?,
    enabled: Boolean,
    loading: Boolean
) {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val alphaAnim by animateFloatAsState(if (enabled) 1f else 0.4f, label = "a")
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && !loading,
        modifier = modifier.height(50.dp).pressScale(source).graphicsLayer { this.alpha = alphaAnim },
        shape = RexMd,
        color = container,
        contentColor = content,
        border = border,
        interactionSource = source
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "btnBase"
            ) { isLoading ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    } else {
                        if (icon != null) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun RexTonalButton(
    text: String, icon: ImageVector?, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false
) {
    val s = MaterialTheme.colorScheme
    RexButtonBase(
        text, icon, onClick, modifier,
        container = s.primary.copy(alpha = 0.14f), content = s.primary,
        border = BorderStroke(1.dp, s.primary.copy(alpha = 0.30f)),
        enabled = enabled, loading = loading
    )
}

@Composable
fun RexOutlinedButton(
    text: String, icon: ImageVector?, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true
) {
    val s = MaterialTheme.colorScheme
    RexButtonBase(
        text, icon, onClick, modifier,
        container = Color.Transparent, content = s.onSurface,
        border = hairline(0.95f), enabled = enabled, loading = false
    )
}

@Composable
fun RexRoundButton(
    icon: ImageVector, contentDescription: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = 52.dp
) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.size(size).pressScale(source, 0.88f),
        shape = CircleShape,
        color = s.surfaceContainerHigh,
        border = hairline(0.9f),
        interactionSource = source
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

@Composable
fun RexQuickAction(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.pressScale(source, 0.93f),
        shape = CircleShape,
        color = s.surfaceContainerHigh.copy(alpha = 0.7f),
        border = hairline(),
        interactionSource = source
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

/* ═════════════════════ PILL, CHIP, STAT ═════════════════════ */

@Composable
fun RexPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    content: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}

@Composable
fun RexFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val container by animateColorAsState(
        if (selected) s.primary else s.surfaceContainerHigh.copy(alpha = 0.6f), tween(260), label = "bg"
    )
    val content by animateColorAsState(if (selected) s.onPrimary else s.onSurfaceVariant, tween(260), label = "fg")
    val borderColor by animateColorAsState(if (selected) Color.Transparent else s.outlineVariant, tween(260), label = "bd")
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.pressScale(source, 0.93f),
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = source
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

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

@Composable
fun RexEmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val amp = with(LocalDensity.current) { 5.dp.toPx() }
    val t = rememberInfiniteTransition(label = "empty")
    val bob = t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val ring = t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "ring")
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        val r = ring.value
                        scaleX = 1f + r * 0.55f
                        scaleY = 1f + r * 0.55f
                        alpha = (1f - r) * 0.55f
                    }
                    .border(1.5.dp, s.primary, CircleShape)
            )
            Box(
                Modifier
                    .size(70.dp)
                    .graphicsLayer { translationY = bob.value * amp }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(s.primary.copy(alpha = 0.18f), s.tertiary.copy(alpha = 0.10f))))
                    .border(1.dp, s.primary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(28.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodySmall, color = s.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/* ── Toast ── */

@Stable
class RexToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    var tick by mutableStateOf(0)
        private set

    fun show(text: String, error: Boolean = false) {
        message = text
        isError = error
        tick++
    }

    fun dismiss() { message = null }
}

@Composable
fun rememberRexToast(): RexToastState = remember { RexToastState() }

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