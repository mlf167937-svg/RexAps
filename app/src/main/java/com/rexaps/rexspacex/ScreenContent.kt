package com.rexaps.rexspacex

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SpaceBlack: Color @Composable get() = MaterialTheme.colorScheme.background
private val SpacePanel: Color @Composable get() = MaterialTheme.colorScheme.surface
private val SpaceBlue: Color @Composable get() = MaterialTheme.colorScheme.primary
private val SpaceRed: Color @Composable get() = MaterialTheme.colorScheme.error
private val SpaceMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val SpaceGreen: Color @Composable get() = MaterialTheme.colorScheme.tertiary

@Composable
fun RexSpaceXScreenContent(onExit: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedMode by rememberSaveable { mutableStateOf("BALANCED") }
    var fpsStabilizer by rememberSaveable { mutableStateOf(true) }
    var touchBoost by rememberSaveable { mutableStateOf(true) }
    var doNotDisturb by rememberSaveable { mutableStateOf(false) }
    var overlayHint by rememberSaveable { mutableStateOf(false) }
    var sensitivity by rememberSaveable { mutableFloatStateOf(0.65f) }

    val modes = listOf("SAVE", "BALANCED", "EXTREME")
    val accent by animateColorAsState(
        targetValue = if (selectedMode == "EXTREME") SpaceRed else SpaceBlue,
        label = "spaceXAccent"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.surfaceVariant, SpaceBlack, MaterialTheme.colorScheme.background)
                )
            )
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (rexIsLandscapeLayout()) 2 else 1),
            modifier = Modifier
                .widthIn(max = if (rexIsLandscapeLayout()) 1280.dp else 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(if (rexIsLandscapeLayout()) 14.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onExit,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("REX", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
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

            item(span = { GridItemSpan(maxLineSpan) }) {
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
                                    listOf(MaterialTheme.colorScheme.surfaceVariant, SpaceBlack, SpacePanel)
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
                                    Text("GAME SPACE", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    Text("BOOST • FOCUS • PLAY", color = SpaceMuted, fontSize = 10.sp, letterSpacing = 1.6.sp)
                                }
                                Icon(Icons.Default.Gamepad, null, tint = accent, modifier = Modifier.size(28.dp))
                            }
                            Text(
                                "Your arena. Your rules.",
                                color = MaterialTheme.colorScheme.onSurface,
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

            item {
                SectionTitle("PERFORMANCE PROFILE", "01")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { mode ->
                        val active = selectedMode == mode
                        Surface(
                            modifier = Modifier.weight(1f).clickable { selectedMode = mode },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) accent.copy(alpha = .18f) else SpacePanel,
                            border = BorderStroke(1.dp, if (active) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
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
                                    color = if (active) MaterialTheme.colorScheme.onSurface else SpaceMuted,
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

            item {
                SectionTitle("QUICK TUNING", "02")
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpacePanel),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .07f))
                ) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        ToggleRow(
                            icon = Icons.Default.Speed,
                            title = "FPS Stabilizer",
                            subtitle = "Preferensi sesi bermain",
                            checked = fpsStabilizer,
                            accent = accent,
                            onCheckedChange = { fpsStabilizer = it }
                        )
                        DividerLine()
                        ToggleRow(
                            icon = Icons.Default.Tune,
                            title = "Touch Enhancement",
                            subtitle = "Profil sensitivitas sentuhan",
                            checked = touchBoost,
                            accent = accent,
                            onCheckedChange = { touchBoost = it }
                        )
                        DividerLine()
                        ToggleRow(
                            icon = Icons.Default.Shield,
                            title = "Focus Mode",
                            subtitle = "Tandai mode minim gangguan",
                            checked = doNotDisturb,
                            accent = accent,
                            onCheckedChange = { doNotDisturb = it }
                        )
                        DividerLine()
                        Column(Modifier.padding(vertical = 13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, null, tint = accent)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Touch profile", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                    Text("${(sensitivity * 100).toInt()}% sensitivity target", color = SpaceMuted, fontSize = 11.sp)
                                }
                            }
                            Slider(
                                value = sensitivity,
                                onValueChange = { sensitivity = it },
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
                                )
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("QUICK LAUNCH", "03")
                GameLaunchCard(
                    title = "Mobile Legends: Bang Bang",
                    subtitle = "Launch game",
                    packageName = "com.mobile.legends",
                    accent = accent
                )
                Spacer(Modifier.height(9.dp))
                GameLaunchCard(
                    title = "Mobile Legends (Alternative)",
                    subtitle = "Try if your MLBB package differs",
                    packageName = "com.mobile.legends.mi",
                    accent = accent
                )
            }

            item {
                SectionTitle("GAMING TOOLS", "04")
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ToolTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.NetworkCheck,
                        title = "Network",
                        subtitle = "Open settings",
                        accent = accent
                    ) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {
                            toast(context, "Pengaturan jaringan tidak tersedia")
                        }
                    }
                    ToolTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MusicNote,
                        title = "Music",
                        subtitle = "Audio controls",
                        accent = accent
                    ) {
                        try {
                            context.startActivity(Intent("android.intent.action.MUSIC_PLAYER").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {
                            toast(context, "Tidak ada pemutar musik yang tersedia")
                        }
                    }
                    ToolTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        title = "System",
                        subtitle = "Display settings",
                        accent = accent
                    ) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {
                            toast(context, "Pengaturan layar tidak tersedia")
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpacePanel),
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
                            Text("RexSpaceX Core", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "UI & quick launch aktif • VPN overlay dan kontrol sistem lanjutan belum diaktifkan",
                                color = SpaceMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, number: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(number, color = SpaceRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)))
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = .22f),
        border = BorderStroke(1.dp, color.copy(alpha = .3f))
    ) {
        Column(Modifier.padding(vertical = 9.dp, horizontal = 8.dp)) {
            Text(label, color = SpaceMuted, fontSize = 9.sp, letterSpacing = 1.sp)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ToggleRow(
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
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = SpaceMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .06f)))
}

@Composable
private fun GameLaunchCard(
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .07f))
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
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (selectedMode == "EXTREME") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                ),
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
private fun ToolTile(
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SpaceMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
