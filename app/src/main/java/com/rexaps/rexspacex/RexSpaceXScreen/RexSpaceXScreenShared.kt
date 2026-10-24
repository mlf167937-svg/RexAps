package com.rexaps.rexspacex

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Every module accent resolves from the active RexAps theme at composition time.
internal val SpaceBlack: Color @Composable get() = MaterialTheme.colorScheme.background

internal val SpacePanel: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

internal val SpaceBlue: Color @Composable get() = MaterialTheme.colorScheme.primary

internal val SpaceRed: Color @Composable get() = MaterialTheme.colorScheme.error

internal val SpaceMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

internal val SpaceGreen: Color @Composable get() = MaterialTheme.colorScheme.tertiary

internal fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
