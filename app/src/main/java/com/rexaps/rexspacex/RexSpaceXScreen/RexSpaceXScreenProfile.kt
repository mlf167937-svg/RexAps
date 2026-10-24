package com.rexaps.rexspacex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RexSpaceXProfile(modes: List<String>, selectedMode: String, accent: Color, onModeSelect: (String) -> Unit) {
        SectionTitle("PERFORMANCE PROFILE", "01")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { mode ->
                val active = selectedMode == mode
                Surface(
                    modifier = Modifier.weight(1f).clickable { onModeSelect(mode) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) accent.copy(alpha = .18f) else SpacePanel,
                    border = BorderStroke(1.dp, if (active) accent else Color.White.copy(alpha = .08f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            when (mode) {
                                "SAVE" -> Icons.Default.Shield
                                "EXTREME" -> Icons.Default.Bolt
                                else -> Icons.Default.Speed
                            },
                            null,
                            tint = if (active) accent else SpaceMuted,
                            modifier = Modifier.size(23.dp)
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            mode,
                            color = if (active) Color.White else SpaceMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Text(
            "Profil ini menyimpan preferensi RexSpaceX. Tidak mengubah clock CPU/GPU secara langsung.",
            color = SpaceMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 7.dp)
        )
}
