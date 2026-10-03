package com.rexaps.rexchat.rexchat

import android.app.Activity
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rexaps.rexchat.RexChatScreen
import com.rexaps.rexchat.SessionManager

/**
 * Activity WebView RexChat.
 *
 * Activity ini berada pada process:
 *
 * :rexchat_webview
 *
 * Session ID diberikan dari MainActivity.
 *
 * Sebelum WebView dibuat, kita wajib melakukan:
 *
 * WebView.setDataDirectorySuffix(...)
 *
 * Ini yang membuat session mempunyai data WebView berbeda.
 */
class RexChatActivity : ComponentActivity() {

    companion object {

        const val EXTRA_SESSION_ID =
            "rexchat_session_id"

        private const val DEFAULT_SESSION_ID = 1
    }

    private var sessionId =
        DEFAULT_SESSION_ID

    private var finishingProcess =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        /*
         * ------------------------------------------------------
         * AMBIL SESSION ID
         * ------------------------------------------------------
         */

        sessionId =
            intent.getIntExtra(
                EXTRA_SESSION_ID,
                DEFAULT_SESSION_ID
            )

        /*
         * ------------------------------------------------------
         * VALIDASI
         * ------------------------------------------------------
         */

        if (sessionId <= 0) {

            finish()

            return
        }

        /*
         * ------------------------------------------------------
         * WEBVIEW DATA DIRECTORY SUFFIX
         * ------------------------------------------------------
         *
         * SANGAT PENTING:
         *
         * Ini harus dilakukan SEBELUM membuat WebView.
         */

        val suffix =
            SessionManager.getWebViewDataSuffix(
                sessionId
            )

        runCatching {

            android.webkit.WebView
                .setDataDirectorySuffix(
                    suffix
                )

        }.onFailure {

            /*
             * Kalau suffix sudah pernah digunakan dalam
             * process yang sama, WebView tidak boleh
             * diinisialisasi lagi dengan suffix lain.
             *
             * Karena Activity ini memang process khusus,
             * kondisi ini biasanya berarti process lama
             * belum benar-benar mati.
             */

            finish()

            return
        }

        /*
         * ------------------------------------------------------
         * SUPER
         * ------------------------------------------------------
         */

        super.onCreate(
            savedInstanceState
        )

        /*
         * ------------------------------------------------------
         * UI
         * ------------------------------------------------------
         */

        setContent {

            RexChatScreen(
                activity = this,
                sessionId = sessionId,

                onExit = {
                    closeWebViewProcess()
                }
            )
        }
    }

    /**
     * Keluar dari session.
     *
     * Kita sengaja mematikan process WebView setelah Activity
     * selesai supaya process tersebut dapat digunakan lagi
     * untuk session ID berbeda.
     */
    private fun closeWebViewProcess() {

        if (finishingProcess) {
            return
        }

        finishingProcess = true

        finishAndRemoveTask()

        window.decorView.postDelayed({

            runCatching {

                Process.killProcess(
                    Process.myPid()
                )
            }

        }, 150L)
    }

    override fun onBackPressed() {

        /*
         * RexChatScreen sudah mempunyai BackHandler.
         *
         * Tetapi fallback tetap disediakan.
         */
        if (isFinishing) {
            return
        }

        super.onBackPressed()
    }
}
