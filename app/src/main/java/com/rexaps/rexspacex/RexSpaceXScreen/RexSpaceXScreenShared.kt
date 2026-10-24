package com.rexaps.rexspacex

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Every module accent resolves from the active RexAps theme at composition time.
internal val SpaceBlack: Color @Composable get() = MaterialTheme.colorScheme.background

internal val SpaceBlue: Color @Composable get() = MaterialTheme.colorScheme.primary

internal val SpaceRed: Color @Composable get() = MaterialTheme.colorScheme.error

internal val SpaceMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

internal val SpaceGreen: Color @Composable get() = MaterialTheme.colorScheme.tertiary

@Composable
internal fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .06f)))
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
