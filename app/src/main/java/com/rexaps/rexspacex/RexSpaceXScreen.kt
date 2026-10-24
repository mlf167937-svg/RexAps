package com.rexaps.rexspacex

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Every module accent resolves from the active RexAps theme at composition time.
internal val SpaceBlack: Color @Composable get() = MaterialTheme.colorScheme.background
internal val SpacePanel: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
internal val SpaceBlue: Color @Composable get() = MaterialTheme.colorScheme.primary
internal val SpaceRed: Color @Composable get() = MaterialTheme.colorScheme.error
internal val SpaceMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val SpaceGreen: Color @Composable get() = MaterialTheme.colorScheme.tertiary

@Composable
fun RexSpaceXScreen(onExit: () -> Unit = {}) {
        return __splitImpl_RexSpaceXScreen(onExit)
    }

@Composable
internal fun SectionTitle(title: String, number: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(number, color = SpaceRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = .12f)))
    }
}

@Composable
internal fun MetricPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = Color.Black.copy(alpha = .22f),
        border = BorderStroke(1.dp, color.copy(alpha = .3f))
    ) {
        Column(Modifier.padding(vertical = 9.dp, horizontal = 8.dp)) {
            Text(label, color = SpaceMuted, fontSize = 9.sp, letterSpacing = 1.sp)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(11.dp), color = accent.copy(alpha = .12f)) {
            Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = SpaceMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .06f)))
}

@Composable
internal fun GameLaunchCard(
    title: String,
    subtitle: String,
    packageName: String,
    accent: Color
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpacePanel),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = .15f)) {
                Icon(Icons.Default.Gamepad, null, tint = accent, modifier = Modifier.padding(11.dp).size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = SpaceMuted, fontSize = 11.sp)
            }
            Button(
                onClick = {
                    try {
                        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            toast(context, "Game tidak ditemukan. Periksa versi/paket game.")
                        }
                    } catch (_: ActivityNotFoundException) {
                        toast(context, "Game tidak bisa dibuka")
                    } catch (_: Exception) {
                        toast(context, "Gagal membuka game")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF06101A)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(Icons.Default.Launch, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("PLAY", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun ToolTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = SpacePanel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .08f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SpaceMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
