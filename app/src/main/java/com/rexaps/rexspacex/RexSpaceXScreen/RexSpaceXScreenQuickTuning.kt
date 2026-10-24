package com.rexaps.rexspacex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RexSpaceXQuickTuning(fpsStabilizer: Boolean, touchBoost: Boolean, doNotDisturb: Boolean, sensitivity: Float, accent: Color, onFpsStabilizerChange: (Boolean) -> Unit, onTouchBoostChange: (Boolean) -> Unit, onDoNotDisturbChange: (Boolean) -> Unit, onSensitivityChange: (Float) -> Unit) {
        SectionTitle("QUICK TUNING", "02")
        Card(
            colors = CardDefaults.cardColors(containerColor = SpacePanel),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ToggleRow(
                    icon = Icons.Default.Speed,
                    title = "FPS Stabilizer",
                    subtitle = "Preferensi sesi bermain",
                    checked = fpsStabilizer,
                    accent = accent,
                    onCheckedChange = onFpsStabilizerChange
                )
                DividerLine()
                ToggleRow(
                    icon = Icons.Default.Tune,
                    title = "Touch Enhancement",
                    subtitle = "Profil sensitivitas sentuhan",
                    checked = touchBoost,
                    accent = accent,
                    onCheckedChange = onTouchBoostChange
                )
                DividerLine()
                ToggleRow(
                    icon = Icons.Default.Shield,
                    title = "Focus Mode",
                    subtitle = "Tandai mode minim gangguan",
                    checked = doNotDisturb,
                    accent = accent,
                    onCheckedChange = onDoNotDisturbChange
                )
                DividerLine()
                Column(Modifier.padding(vertical = 13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = accent)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Touch profile", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${(sensitivity * 100).toInt()}% sensitivity target", color = SpaceMuted, fontSize = 11.sp)
                        }
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = onSensitivityChange,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = Color.White.copy(alpha = .12f)
                        )
                    )
                }
            }
        }
}
