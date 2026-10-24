package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun StoryRingAvatar(
    acct: RexLocalAccount,
    rev: Int,
    storyCount: Int,
    dia: Dp,
    modifier: Modifier,
    animate: Boolean = true,
    onClick: () -> Unit,
    onAdd: (() -> Unit)? = null
) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "ring")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "angle")
    val bob by inf.animateFloat(-3f, 3f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val pop by animateFloatAsState(if (appear) 1f else .2f, spring(Spring.DampingRatioMediumBouncy), label = "pop")
    val ringColors = listOf(c.primary, c.tertiary, c.secondary, c.primary)

    Box(
        modifier
            .size(dia)
            .graphicsLayer {
                translationY = if (animate) bob * density else 0f
                scaleX = pop
                scaleY = pop
            }
            .drawBehind {
                val sw = 3.5.dp.toPx()
                val d = this.size.minDimension
                rotate(angle) {
                    val brush = Brush.sweepGradient(ringColors)
                    if (storyCount in 2..12) {
                        val seg = 360f / storyCount
                        for (i in 0 until storyCount) {
                            drawArc(
                                brush, -90f + i * seg + 5f, seg - 10f, false,
                                Offset(sw / 2, sw / 2), Size(d - sw, d - sw),
                                style = Stroke(sw, cap = StrokeCap.Round)
                            )
                        }
                    } else {
                        drawCircle(
                            brush, radius = d / 2 - sw / 2,
                            style = Stroke(sw),
                            alpha = if (storyCount == 0) .5f else 1f
                        )
                    }
                }
            }
    ) {
        AvatarContent(acct, rev, animate, Modifier.fillMaxSize().padding(7.dp).pressable(onClick))
        if (onAdd != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(c.primary)
                    .border(3.dp, c.background, CircleShape)
                    .pressable(onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun StoryTile(st: RexStory, acct: RexLocalAccount, rev: Int, modifier: Modifier, onClick: () -> Unit) {
    val left = (DAY_MS - (System.currentTimeMillis() - st.time)).coerceAtLeast(0)
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.pressable(onClick).clip(shape).border(1.dp, Color.White.copy(alpha = .08f), shape)) {
        if (st.isVideo) {
            MediaThumb(st.file, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(storyGradients[st.gradient.mod(storyGradients.size)])
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    st.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black.copy(.35f), .3f to Color.Transparent, .7f to Color.Transparent, 1f to Color.Black.copy(.6f))
        )))
        AvatarContent(
            acct, rev, false,
            Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).border(2.dp, Color.White, CircleShape)
        )
        Text(
            "${left / 3_600_000}j lagi",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
    }
}
