package com.rexaps.rexcoder

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rexaps.rexcoder.ui.RexCoderApp

/**
 * Host screen used by the RexAps shell.
 *
 * The Activity parameter is kept for compatibility with the RexAps router.
 * Workspace and editor state are owned by RexCoderApp.
 */
@Composable
fun RexCoderScreen(
    activity: Activity,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        RexCoderApp(modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White
            )
        }
    }
}
