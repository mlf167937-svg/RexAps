package com.rexaps.ui

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import com.rexaps.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val uri = Uri.parse(
                        "android.resource://${context.packageName}/${R.raw.splash_video}"
                    )
                    setVideoURI(uri)

                    setOnPreparedListener { mp ->
                        mp.isLooping = false

                        // Percepat 1.5x (butuh API 23+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                mp.playbackParams = mp.playbackParams.setSpeed(1.5f)
                            } catch (e: Exception) {
                                // Kalau gagal, jalan normal aja
                            }
                        }

                        start()
                    }

                    setOnCompletionListener {
                        onFinished()
                    }

                    setOnErrorListener { _, _, _ ->
                        // Kalau video error, langsung lanjut
                        onFinished()
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Tombol Skip di pojok kanan bawah
        Button(
            onClick = { onFinished() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x80000000),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Skip  ›",
                fontSize = 14.sp
            )
        }
    }
}