package com.rexaps.rexspacex

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RexSpaceXHero(selectedMode: String, accent: Color) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = .65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF152B49), Color(0xFF101521), Color(0xFF25111E))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = accent.copy(alpha = .16f),
                            border = BorderStroke(1.dp, accent.copy(alpha = .6f))
                        ) {
                            Icon(
                                Icons.Default.Bolt, null, tint = accent,
                                modifier = Modifier.padding(12.dp).size(30.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("GAME SPACE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("BOOST • FOCUS • PLAY", color = SpaceMuted, fontSize = 10.sp, letterSpacing = 1.6.sp)
                        }
                        Icon(Icons.Default.Gamepad, null, tint = accent, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        "Your arena. Your rules.",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Pilih profil permainan dan buka game dari satu panel.",
                        color = SpaceMuted,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("MODE", selectedMode, accent, Modifier.weight(1f))
                        MetricPill("ANDROID", Build.VERSION.RELEASE ?: "?", SpaceGreen, Modifier.weight(1f))
                        MetricPill("SDK", Build.VERSION.SDK_INT.toString(), SpaceBlue, Modifier.weight(1f))
                    }
                }
            }
        }
}
