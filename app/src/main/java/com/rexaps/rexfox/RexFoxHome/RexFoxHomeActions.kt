package com.rexaps.rexfox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
