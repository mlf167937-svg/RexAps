package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rexaps.rexfox.rexPressable

@Composable
internal fun RexPanelScreenConnectFooter(
    busy: Boolean,
    accent: Brush,
    onSubmit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
        Spacer(
            Modifier.height(20.dp)
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .alpha(
                    if (busy) 0.6f else 1f
                )
                .semantics(
                    mergeDescendants = true
                ) {
                    role = Role.Button

                    if (busy) {
                        disabled()
                    }
                }
                .rexPressable(
                    enabled = !busy
                ) {
                    onSubmit()
                }
                .clip(CircleShape)
                .background(accent),
            contentAlignment =
                Alignment.Center
        ) {

            if (busy) {

                CircularProgressIndicator(
                    Modifier.size(22.dp),
                    color =
                        colors.onPrimary,
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    "Hubungkan",
                    color =
                        colors.onPrimary,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "Password tidak disimpan. Verifikasi host key belum aktif di versi ini.",
            style =
                MaterialTheme.typography
                    .labelSmall,
            color =
                colors.onSurfaceVariant
        )
}
