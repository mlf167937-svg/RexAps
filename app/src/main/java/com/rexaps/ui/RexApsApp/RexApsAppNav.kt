package com.rexaps.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_ThemeTabContent(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onOpenTheme: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tema", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personalisasikan tampilan RexAps dengan 30 pilihan tema.", color = colors.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTheme),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ColorLens, null, tint = colors.onPrimary, modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Tema aktif", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(selectedTheme.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ketuk untuk memilih tema", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
