package com.rexaps.rexspacex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RexSpaceXHeader(onExit: () -> Unit = {}) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onExit,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.07f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("REX", color = Color.White, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                Text("SPACEX", color = SpaceBlue, fontSize = 25.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpaceBlue.copy(alpha = .12f),
                border = BorderStroke(1.dp, SpaceBlue.copy(alpha = .45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(SpaceGreen))
                    Spacer(Modifier.width(7.dp))
                    Text("READY", color = SpaceGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
}
