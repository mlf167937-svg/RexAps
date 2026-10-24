package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_ProHomeHeader(
    appCount: Int,
    availableCount: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(7.dp).background(c.primary, CircleShape))
                Text(
                    text = "Selamat datang di",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onSurfaceVariant
                )
            }
            Text(
                text = "RexAps",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$appCount aplikasi · $availableCount siap dibuka",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant
            )
        }

        ProGlass(
            modifier = Modifier
                .size(52.dp)
                .proPress(onClick = onOpenSettings)
                .semantics { contentDescription = "Buka pengaturan" },
            radius = 26.dp
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.align(Alignment.Center).size(24.dp)
            )
        }
    }
}

@Composable
internal fun __splitImpl_ProSectionHeader(title: String, subtitle: String, count: Int) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
        ProGlass(radius = 50.dp) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = c.primary
            )
        }
    }
}
