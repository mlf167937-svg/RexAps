package com.rexaps.rexchat.v1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import com.rexaps.rexchat.SessionManager

class RexChatActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SESSION_ID = "rexchat_session_id"
        const val EXTRA_METHOD = "rexchat_method"
    }

    private var sessionId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
        val session = SessionManager.get(this, sessionId)
        if (session == null) {
            finish()
            return
        }
        SessionManager.touch(this, sessionId)
        RexChatRuntime.start(this, sessionId)
        setContent {
            com.rexaps.ui.RexTheme(option = com.rexaps.ui.RexThemeStore(this@RexChatActivity).load()) {
                RexChatScreen(
                    sessionName = session.name,
                    sessionId = sessionId,
                    initialMethod = intent.getStringExtra(EXTRA_METHOD).orEmpty(),
                    onBack = { finish() },
                    onDelete = {
                        SessionManager.markPendingDelete(this, sessionId)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        if (sessionId > 0) RexChatRuntime.stopSession(sessionId)
        super.onDestroy()
    }
}
