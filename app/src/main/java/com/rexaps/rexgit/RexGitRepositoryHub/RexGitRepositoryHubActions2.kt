@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rexaps.rexgit

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun OpenWebButton(url: String) {
    val context = LocalContext.current
    if (url.isNotBlank()) {
        OutlinedButton(
            onClick = {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Throwable) { }
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 6.dp),
            modifier = Modifier.heightIn(min = 34.dp)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Open on GitHub", modifier = Modifier.size(15.dp))
        }
    }
}
