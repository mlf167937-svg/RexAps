package com.rexaps.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource\nimport com.rexaps.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.delay

private val rexBannerSlides = listOf(
    R.drawable.rex_banner_slide1,
    R.drawable.rex_banner_slide2,
    R.drawable.rex_banner_slide3
)

/** Three-image promo carousel; advances every 15 seconds and supports horizontal swipes. */
@Composable
fun RexBannerCarousel(modifier: Modifier = Modifier) {
    var currentSlide by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentSlide) {
        delay(15_000L)
        currentSlide = (currentSlide + 1) % rexBannerSlides.size
    }

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.98f)
                .clip(RoundedCornerShape(22.dp))
                .semantics { contentDescription = "Banner promosi RexAps, slide ${currentSlide + 1} dari 3" }
                .pointerInput(currentSlide) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount -> dragTotal += amount },
                        onDragEnd = {
                            if (dragTotal < -35f) currentSlide = (currentSlide + 1) % rexBannerSlides.size
                            else if (dragTotal > 35f) currentSlide = (currentSlide - 1 + rexBannerSlides.size) % rexBannerSlides.size
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                }
        ) {
            Crossfade(
                targetState = currentSlide,
                animationSpec = tween(420),
                label = "rexBannerSlide"
            ) { index ->
                Image(
                    painter = painterResource(rexBannerSlides[index]),
                    contentDescription = "Banner RexAps slide ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rexBannerSlides.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == currentSlide) 20.dp else 7.dp, height = 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentSlide) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                        .clickable { currentSlide = index }
                )
            }
        }
    }
}
