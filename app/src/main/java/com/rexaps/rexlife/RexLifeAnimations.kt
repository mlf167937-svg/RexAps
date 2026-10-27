package com.rexaps.rexlife

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Animasi entrance untuk card dengan stagger effect.
 * Digunakan untuk list items yang muncul bersamaan.
 */
fun getCardEntranceAnimation(index: Int): EnterTransition {
    val delay = index * 50
    return slideInVertically(
        animationSpec = tween(500, delayMillis = delay, easing = EaseOutCubic),
        initialOffsetY = { it }
    ) + fadeIn(animationSpec = tween(500, delayMillis = delay))
}

fun getCardExitAnimation(index: Int): ExitTransition {
    return slideOutVertically(
        animationSpec = tween(300, easing = EaseInCubic),
        targetOffsetY = { -it }
    ) + fadeOut(animationSpec = tween(300))
}

/**
 * Bounce scale animation untuk button tap feedback.
 */
fun scaledClickModifier(
    isPressed: Boolean,
    scale: Float = 0.95f
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(dampingRatio = SpringDampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "ScalePress"
    )
    return Modifier.scale(animatedScale)
}

/**
 * Rotating progress indicator untuk loading states.
 */
@Composable
fun RotatingProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RotatingProgress")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            strokeWidth = 2.dp,
            color = color
        )
    }
}

/**
 * Floating action label yang muncul saat user hover/tap.
 */
@Composable
fun AnimatedFloatingLabel(
    isVisible: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = tween(300, easing = EaseOutCubic),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            animationSpec = tween(200, easing = EaseInCubic),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onTertiary
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Pulse animation untuk highlight urgent items.
 */
@Composable
fun PulseAnimation(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = if (isActive) alpha else 1f
        }
    ) {
        content()
    }
}

/**
 * Number counter animation untuk balance/target amount.
 */
@Composable
fun AnimatedNumberCounter(
    value: Long,
    modifier: Modifier = Modifier,
    formatter: (Long) -> String = { formatRupiah(it) }
) {
    val animatedValue by animateLongAsState(
        targetValue = value,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "CounterAnimation"
    )

    Text(
        formatter(animatedValue),
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium
    )
}

/**
 * Shimmer loading effect untuk skeleton screens.
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!isLoading) {
        content()
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

/**
 * Progress bar animation dengan smooth easing.
 */
@Composable
fun SmoothProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "ProgressAnimation"
    )

    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = modifier,
        color = color,
        trackColor = color.copy(alpha = 0.2f)
    )
}

/**
 * Height expansion animation untuk collapsible sections.
 */
fun expandHeightAnimation(targetHeight: Float): Animatable<Float, AnimationVector1D> {
    return Animatable(0f)
}

/**
 * Shake animation untuk error states.
 */
@Composable
fun ShakeAnimation(
    trigger: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    var shake by remember(trigger) { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            shake = true
            delay(100)
            shake = false
        }
    }

    val xOffset by animateDpAsState(
        targetValue = if (shake) 4.dp else 0.dp,
        animationSpec = tween(50, easing = EaseInOut),
        label = "ShakeX"
    )

    content(modifier.offset(x = xOffset))
}

/**
 * Swipe to dismiss animation.
 */
@Composable
fun DismissibleCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isDismissed by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isDismissed,
        exit = slideOutHorizontally(
            animationSpec = tween(300, easing = EaseInCubic),
            targetOffsetX = { it }
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        content()
    }

    if (isDismissed) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
    }
}

/**
 * Animated size change untuk expanding cards.
 */
@Composable
fun AnimatedHeightContent(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = tween(300, easing = EaseOutCubic)),
        exit = shrinkVertically(animationSpec = tween(300, easing = EaseInCubic)),
        modifier = modifier
    ) {
        content()
    }
}

private val CircularProgressIndicator = @Composable { modifier: Modifier ->
    Box(
        modifier = modifier
            .background(
                color = Color.Transparent,
                shape = CircleShape
            )
    )
}

private val Text = @Composable { text: String, modifier: Modifier, style: androidx.compose.material3.TextStyle ->
    androidx.compose.material3.Text(text, modifier = modifier, style = style)
}

private val LinearProgressIndicator = @Composable { progress: Float, modifier: Modifier, color: Color, trackColor: Color ->
    androidx.compose.material3.LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor)
}

private val Surface = @Composable { shape: androidx.compose.foundation.shape.RoundedCornerShape, color: Color, contentColor: Color, content: @Composable () -> Unit ->
    androidx.compose.material3.Surface(shape = shape, color = color, contentColor = contentColor, content = content)
}
