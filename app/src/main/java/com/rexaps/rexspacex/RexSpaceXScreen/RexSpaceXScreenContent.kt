package com.rexaps.rexspacex

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RexSpaceXScreenContent(onExit: () -> Unit = {}) {
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
                    colors = listOf(SpacePanel.copy(alpha = 0.76f), SpaceBlack.copy(alpha = 0.86f), SpaceBlack.copy(alpha = 0.91f))
                )
            )
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
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

            item {
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

            item {
                SectionTitle("PERFORMANCE PROFILE", "01")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { mode ->
                        val active = selectedMode == mode
                        Surface(
                            modifier = Modifier.weight(1f).clickable { selectedMode = mode },
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

            item {
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
                                    Text("Touch profile", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("${(sensitivity * 100).toInt()}% sensitivity target", color = SpaceMuted, fontSize = 11.sp)
                                }
                            }
                            Slider(
                                value = sensitivity,
                                onValueChange = { sensitivity = it },
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
        }
    }
}
