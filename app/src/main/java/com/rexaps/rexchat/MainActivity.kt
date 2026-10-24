package com.rexaps.rexchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            com.rexaps.ui.RexTheme(option = com.rexaps.ui.RexThemeStore(this@MainActivity).load()) {
                SessionListScreen(
                    refreshTick = tick,
                    onOpen = { session, method ->
                        openRexChatSession(this, session, method)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SessionManager.purgePending(this)
        tick++
    }
}

// =====================================================================
// SESSION LIST SCREEN
// =====================================================================
