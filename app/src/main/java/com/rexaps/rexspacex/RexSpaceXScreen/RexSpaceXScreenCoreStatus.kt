package com.rexaps.rexspacex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RexSpaceXCoreStatus() {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1522)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SpaceBlue.copy(alpha = .28f))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = SpaceGreen, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("RexSpaceX Core", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "UI & quick launch aktif • VPN overlay dan kontrol sistem lanjutan belum diaktifkan",
                        color = SpaceMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
}
