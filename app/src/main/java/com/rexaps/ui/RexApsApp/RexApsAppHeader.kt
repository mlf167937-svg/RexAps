// RexApsApp.kt
package com.rexaps.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SectionHeader(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 20.dp)
                .background(accentBrush(), RoundedCornerShape(50))
        )
        Spacer(Modifier.size(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun HomeHeader(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colors.primary, CircleShape)
                )
                Text(
                    text = "Selamat datang di",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
            Text(
                text = "RexAps",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = colors.primary.copy(alpha = 0.4f),
                    spotColor = colors.primary.copy(alpha = 0.4f)
                )
                .pressable(onClick = onOpenSettings)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outlineVariant, CircleShape)
                .semantics { contentDescription = "Buka pengaturan" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Pengaturan",
                tint = colors.primary,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
internal fun HeroCard(
    appCount: Int,
    availableCount: Int,
    themeLabel: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary

    val transition = rememberInfiniteTransition(label = "hero")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(accentBrush())
            .drawBehind {
                drawCircle(
                    color = onAccent.copy(alpha = 0.14f),
                    radius = 120.dp.toPx(),
                    center = Offset(
                        x = size.width - 30.dp.toPx() - drift * 30.dp.toPx(),
                        y = 10.dp.toPx() + drift * 24.dp.toPx()
                    )
                )
                drawCircle(
                    color = onAccent.copy(alpha = 0.10f),
                    radius = 70.dp.toPx(),
                    center = Offset(
                        x = size.width - 90.dp.toPx() + drift * 36.dp.toPx(),
                        y = size.height - 10.dp.toPx() - drift * 20.dp.toPx()
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(onAccent.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$appCount aplikasi",
                    style = MaterialTheme.typography.labelMedium,
                    color = onAccent
                )
            }

            Text(
                text = "Semua aplikasimu,\nsatu tempat.",
                style = MaterialTheme.typography.headlineSmall,
                color = onAccent
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStatChip(label = "Tersedia", value = "$availableCount", onAccent = onAccent)
                HeroStatChip(label = "Tema aktif", value = themeLabel, onAccent = onAccent)
            }
        }
    }
}

@Composable
internal fun HeroStatChip(label: String, value: String, onAccent: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(onAccent.copy(alpha = 0.12f))
            .border(1.dp, onAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onAccent.copy(alpha = 0.8f)
        )
    }
}
