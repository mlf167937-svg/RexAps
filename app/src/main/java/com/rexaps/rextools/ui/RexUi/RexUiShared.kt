// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val RexSm = RoundedCornerShape(12.dp)

val RexMd = RoundedCornerShape(18.dp)

val RexLg = RoundedCornerShape(26.dp)

/** Easing "ease-out expo": cepat di awal, mendarat sangat halus. */
val RexEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun hairline(alpha: Float = 0.7f) =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))

/* ═════════════════════ THEME (opsional) ═════════════════════ */

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
