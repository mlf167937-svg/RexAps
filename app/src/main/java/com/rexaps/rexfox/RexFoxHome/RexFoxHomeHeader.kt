package com.rexaps.rexfox

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
