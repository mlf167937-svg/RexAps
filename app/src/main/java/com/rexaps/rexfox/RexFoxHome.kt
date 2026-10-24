package com.rexaps.rexfox

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RexFoxHome(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onTabs: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
        return __splitImpl_RexFoxHome(state, onNavigate, onTabs, onBookmarks, onHistory, onDownloads, onSettings)
    }

/** Desktop-like wide layout: navigation/search on the left, activity lists on the right. */
@Composable
internal fun RexFoxHomeLandscape(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onTabs: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
        return __splitImpl_RexFoxHomeLandscape(state, onNavigate, onTabs, onBookmarks, onHistory, onDownloads, onSettings)
    }

@Composable
internal fun HomeHeader(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(RexFoxAccent),
            contentAlignment = Alignment.Center
        ) {
            Text("R", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text("RexFox", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text("Fast. Focused. Dev-ready.", color = OnSurfaceMuted, fontSize = 12.sp)
        }

        Box(
            Modifier
                .size(46.dp)
                .rexPressable(onClick = onSettings)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = OnSurfacePrimary)
        }
    }
}

@Composable
internal fun PrivacyCard(
    status: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    val transition = rememberInfiniteTransition(label = "privacy")
    val pulse by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .border(
                1.dp,
                Brush.linearGradient(listOf(Cyan.copy(alpha = 0.2f + pulse * 0.6f), BorderSubtle)),
                shape
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(Cyan.copy(alpha = pulse * 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, null, tint = Cyan)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Privacy status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(status, color = OnSurfaceMuted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun RowScope.QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Column(
        Modifier
            .weight(1f)
            .rexPressable(onClick = onClick)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, shape)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, maxLines = 1, color = OnSurfacePrimary)
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 6.dp),
        color = VioletLight,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
internal fun Entry(
    title: String,
    url: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .rexPressable(onClick = onClick)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(SurfaceElevated, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = VioletLight, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title.ifBlank { url },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                color = OnSurfaceMuted
            )
        }
    }
}
