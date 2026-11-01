// setting.kt
package com.rexaps.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rexaps.ui.theme.RexAnimationLevel
import com.rexaps.ui.theme.RexThemeManager

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Model tautan sosial resmi Rex Studio.
 * Nama dibuat unik (RexSettingsSocial / rexSettingsSocials) agar tidak
 * bentrok dengan deklarasi lain di package com.rexaps.ui.
 */
private data class RexSettingsSocial(
    val title: String,
    val subtitle: String,
    val symbol: String,
    val color: Color,
    val url: String
)

private val rexSettingsSocials = listOf(
    RexSettingsSocial(
        title = "YouTube",
        subtitle = "@RexRuinCraft",
        symbol = "▶",
        color = Color(0xFFFF0000),
        url = "https://www.youtube.com/@RexRuinCraft"
    ),
    RexSettingsSocial(
        title = "TikTok",
        subtitle = "@rexstudio4",
        symbol = "♪",
        color = Color(0xFF111111),
        url = "https://www.tiktok.com/@rexstudio4"
    ),
    RexSettingsSocial(
        title = "Saweria",
        subtitle = "Dukung lewat donasi",
        symbol = "☕",
        color = Color(0xFFF5A623),
        url = "https://saweria.co/rexruincraft"
    ),
    RexSettingsSocial(
        title = "Saluran WhatsApp",
        subtitle = "Gabung untuk update terbaru",
        symbol = "✆",
        color = Color(0xFF25D366),
        url = "https://whatsapp.com/channel/0029Vb8oqCTDOQIYrRuWic33"
    )
)

@Composable
fun RexSettingsScreen(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onOpenTheme: () -> Unit,
    onBackHome: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val motionManager = remember(context) { RexThemeManager(context) }
    val motionSettings = motionManager.motionSettings
    val animationEnabled = motionSettings.level != RexAnimationLevel.OFF

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        /* ── Header ── */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant.copy(alpha = .7f))
                    .border(1.dp, colors.outlineVariant.copy(alpha = .5f), CircleShape)
                    .clickable(onClick = onBackHome),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Kembali ke beranda",
                    tint = colors.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Pengaturan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Atur pengalaman RexAps kamu",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            }
        }

        /* ── Hero brand card ── */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "RexAps",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onPrimary
                )
                Text(
                    "Satu tempat untuk aplikasi dan tools digital.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimary.copy(alpha = .88f)
                )
                Surface(
                    shape = CircleShape,
                    color = colors.onPrimary.copy(alpha = .18f)
                ) {
                    Text(
                        "All in One Apps",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimary
                    )
                }
            }
        }

        /* ── Personalisasi ── */
        SettingsSectionTitle(
            title = "Personalisasi",
            subtitle = "Atur tampilan aplikasi sesuai seleramu"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTheme),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceVariant.copy(alpha = .6f),
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .5f)),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Tema aplikasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Aktif: ${selectedTheme.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Ubah warna dan suasana tampilan",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
            }
        }

        /* ── Animation & performance ── */
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsSectionTitle(
                title = "Animation & Performance",
                subtitle = "Pengaturan global untuk seluruh RexAps dan semua modul"
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceVariant.copy(alpha = .58f),
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .65f))
            ) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(
                        "Berlaku untuk UI utama dan seluruh modul RexAps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = .14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = colors.primary)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Animation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (animationEnabled) "Aktif · ${when (motionSettings.level) { RexAnimationLevel.SAVER -> "Low"; RexAnimationLevel.BALANCED -> "Medium"; RexAnimationLevel.FULL -> "High"; RexAnimationLevel.OFF -> "Off" }}"
                                else "Nonaktif · mode hemat animasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = animationEnabled,
                            onCheckedChange = { enabled ->
                                val nextLevel = if (enabled) {
                                    if (motionSettings.level == RexAnimationLevel.OFF) RexAnimationLevel.BALANCED else motionSettings.level
                                } else RexAnimationLevel.OFF
                                motionManager.updateMotion(motionSettings.copy(level = nextLevel))
                            }
                        )
                    }
                    if (animationEnabled) {
                        Text("ADVANCED ANIMATION", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                RexAnimationLevel.SAVER to "LOW",
                                RexAnimationLevel.BALANCED to "MEDIUM",
                                RexAnimationLevel.FULL to "HIGH"
                            ).forEach { (level, label) ->
                                FilterChip(
                                    selected = motionSettings.level == level,
                                    onClick = { motionManager.updateMotion(motionSettings.copy(level = level)) },
                                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer
                                    )
                                )
                            }
                        }
                        Text(
                            when (motionSettings.level) {
                                RexAnimationLevel.SAVER -> "Low: animasi ringan dan jumlah efek visual dikurangi."
                                RexAnimationLevel.BALANCED -> "Medium: transisi halus dengan efek visual seimbang."
                                RexAnimationLevel.FULL -> "High: efek visual lebih kaya; penggunaan resource bisa meningkat."
                                RexAnimationLevel.OFF -> "Animasi nonaktif."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Animasi dekoratif dan gerakan background dihentikan. Indikator proses penting tetap ditampilkan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        /* ── Tentang & sosial ── */
        SettingsSectionTitle(
            title = "Tentang RexAps",
            subtitle = "Satu tempat untuk aplikasi dan tools digital."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Text(
                "Terhubung dengan Rex Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rexSettingsSocials.forEach { link ->
                SocialCard(
                    link = link,
                    onClick = { runCatching { uriHandler.openUri(link.url) } }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "RexAps • All in One Apps",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SocialCard(link: RexSettingsSocial, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(link.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = link.symbol,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    link.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("↗", style = MaterialTheme.typography.titleSmall, color = colors.primary)
            }
        }
    }
}
