package com.rexaps.rexfox

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Semua warna RexFox sekarang mengikuti tema RexAps yang sedang aktif.
val Violet: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val VioletLight: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val Cyan: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary
val CyanLight: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary
val DeepBlack: Color
    @Composable get() = MaterialTheme.colorScheme.background
val SurfaceDark: Color
    @Composable get() = lerp(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surfaceVariant,
        0.5f
    )
val SurfaceCard: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val SurfaceElevated: Color
    @Composable get() = lerp(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurface,
        0.07f
    )
val BorderSubtle: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant
val OnSurfaceMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val OnSurfacePrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

/** Warna teks/ikon di atas gradien aksen (menggantikan Color.White). */
val OnAccent: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

val RexFoxAccent: Brush
    @Composable get() = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )

/** Tidak lagi memaksa warna sendiri, hanya mewarisi tema dari RexAps. */
@Composable
fun RexFoxTheme(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content
    )
}

/* ------------------------------ ANIMATION KIT ----------------------------- */

/** Elemen muncul bergantian: fade + naik + zoom kecil. */
@Composable
fun Modifier.rexEntrance(index: Int): Modifier {
    var shown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!shown) delay(index.coerceAtMost(8) * 55L)
        shown = true
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "rexEntrance"
    )

    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 36.dp.toPx()
        val s = 0.95f + 0.05f * progress
        scaleX = s
        scaleY = s
    }
}

/** Klik dengan efek mengecil memantul saat ditekan. */
@Composable
fun Modifier.rexPressable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rexPress"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

/** Transisi halus saat berpindah layar RexFox. */
@Composable
fun RexFoxFade(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "rexScreen"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress
                val s = 0.985f + 0.015f * progress
                scaleX = s
                scaleY = s
            }
    ) {
        content()
    }
}
