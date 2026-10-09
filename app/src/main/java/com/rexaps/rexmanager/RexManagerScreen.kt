package com.rexaps.rexmanager

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val RexNight = Color(0xFF070B14)
private val RexBlue = Color(0xFF2585FF)
private val RexViolet = Color(0xFF7657FF)
private val RexCyan = Color(0xFF36D7F2)

/** Public feature entry point. The first visit opens a swipe-to-start welcome screen. */
@Composable
fun RexManagerScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var started by remember { mutableStateOf(false) }
    if (started) {
        RexManagerWorkspace(onExit = onExit, modifier = modifier)
    } else {
        RexManagerWelcomeScreen(onStart = { started = true }, modifier = modifier)
    }
}

@Composable
private fun RexManagerWelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "rex-welcome")
    val glow by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF080D19), RexNight, Color(0xFF071323)))
        )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val swipeAvailableWidth = maxWidth - 56.dp
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Brush.radialGradient(listOf(RexBlue.copy(alpha = glow * .32f), Color.Transparent), center = Offset(w * .12f, h * .18f), radius = w * .72f), radius = w * .72f, center = Offset(w * .12f, h * .18f))
            drawCircle(Brush.radialGradient(listOf(RexViolet.copy(alpha = glow * .24f), Color.Transparent), center = Offset(w * .92f, h * .78f), radius = w * .68f), radius = w * .68f, center = Offset(w * .92f, h * .78f))
            val wave = Path()
            wave.moveTo(0f, h * .74f)
            wave.cubicTo(w * .23f, h * .61f, w * .35f, h * .93f, w * .62f, h * .78f)
            wave.cubicTo(w * .78f, h * .69f, w * .86f, h * .68f, w, h * .57f)
            drawPath(wave, Brush.horizontalGradient(listOf(RexBlue.copy(alpha = .02f), RexBlue.copy(alpha = .28f), RexViolet.copy(alpha = .20f))), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(118.dp).clip(RoundedCornerShape(34.dp)).background(Brush.linearGradient(listOf(RexBlue, RexViolet))).border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("R", fontSize = 82.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 86.sp)
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(22.dp))
                }
                Text("RexManager", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF3F7FF), letterSpacing = (-.8).sp)
                Text("Kelola File Tanpa Batas", fontSize = 15.sp, color = RexCyan, fontWeight = FontWeight.Medium)
                Text("Semua file. Semua arsip. Satu tempat.", fontSize = 14.sp, color = Color(0xFFA7B5CA), textAlign = TextAlign.Center)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Cepat  •  Modern  •  Terorganisir", color = Color(0xFFA7B5CA), fontSize = 12.sp, textAlign = TextAlign.Center)
                SwipeToStart(onStart = onStart, availableWidth = swipeAvailableWidth)
                Text("Geser tombol ke kanan untuk mulai", color = Color(0xFF8798B2), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SwipeToStart(onStart: () -> Unit, availableWidth: androidx.compose.ui.unit.Dp) {
    var drag by remember { mutableFloatStateOf(0f) }
    var maxDrag by remember { mutableFloatStateOf(0f) }
    val animatedDrag by animateFloatAsState(drag, animationSpec = tween(180), label = "swipe-position")
    val hint = rememberInfiniteTransition(label = "swipe-hint")
    val hintAlpha by hint.animateFloat(.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "swipe-alpha")
    BoxWithConstraints(
        modifier = Modifier.width(availableWidth.coerceAtMost(420.dp)).height(62.dp).clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF17243A), Color(0xFF101A2B))))
            .border(1.dp, Color(0xFF344B6B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val thumbSize = 50.dp
        val trackWidth = with(density) { constraints.maxWidth.toFloat() }
        val thumbPx = with(density) { thumbSize.toPx() }
        val maxOffset = (trackWidth - thumbPx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)
        maxDrag = maxOffset
        Text("MULAI", color = Color.White.copy(alpha = .78f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 2.sp)
        Icon(Icons.Default.Swipe, contentDescription = null, tint = RexCyan.copy(alpha = hintAlpha), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(20.dp))
        Box(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).offset { IntOffset(animatedDrag.roundToInt(), 0) }
                .size(thumbSize).clip(CircleShape).background(Brush.linearGradient(listOf(RexBlue, RexViolet)))
                .border(1.dp, Color.White.copy(alpha = .45f), CircleShape)
                .pointerInput(maxOffset) {
                    detectDragGestures(
                        onDragEnd = {
                            if (drag >= maxDrag * .72f) onStart()
                            else drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            drag = (drag + amount.x).coerceIn(0f, maxOffset)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.ArrowForward, contentDescription = "Geser untuk mulai", tint = Color.White, modifier = Modifier.size(24.dp)) }
    }
}
