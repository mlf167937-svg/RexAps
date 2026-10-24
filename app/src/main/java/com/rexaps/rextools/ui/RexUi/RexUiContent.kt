package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_RexPrimaryButton(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun __splitImpl_RexScreen(
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
