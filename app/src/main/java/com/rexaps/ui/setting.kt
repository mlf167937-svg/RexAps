package com.rexaps.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pengaturan utama RexAps. Ganti URL sosial di SOCIAL_LINKS dengan tautan akun resmi
 * sebelum rilis; tautan default hanya membuka halaman platform.
 */
private val SOCIAL_LINKS = listOf(
    Triple("GitHub", "Kode & proyek", "https://github.com/"),
    Triple("Instagram", "Kabar dan visual", "https://www.instagram.com/"),
    Triple("YouTube", "Video dan update", "https://www.youtube.com/")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp).clip(CircleShape).clickable(onClick = onBackHome),
                color = colors.surfaceVariant,
                border = BorderStroke(1.dp, colors.outlineVariant),
                shape = CircleShape
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke beranda")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Pengaturan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Atur pengalaman RexAps kamu", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Icon(Icons.Default.Settings, contentDescription = null, tint = colors.primary, modifier = Modifier.size(25.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTheme),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(1.dp, colors.outlineVariant),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(17.dp))
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tema aplikasi", style = MaterialTheme.typography.titleMedium)
                    Text("Tema aktif: ${selectedTheme.label}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text("Ubah warna dan suasana tampilan", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Text("›", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tentang RexAps", style = MaterialTheme.typography.titleLarge)
            Text("Satu tempat untuk aplikasi dan tools digital.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = colors.primary)
            Text("Terhubung dengan Rex Studio", style = MaterialTheme.typography.titleMedium)
        }

        Text("Sosial media", style = MaterialTheme.typography.titleLarge)
        Text("Ikuti kabar, karya, dan pengembangan RexAps.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

        SOCIAL_LINKS.forEach { (name, description, url) ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    runCatching { uriHandler.openUri(url) }
                },
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = RoundedCornerShape(12.dp), color = colors.primaryContainer, modifier = Modifier.size(42.dp)) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (name == "GitHub") Icons.Default.Code else Icons.Default.Language,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Text("↗", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("RexAps • All in One Apps", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}
