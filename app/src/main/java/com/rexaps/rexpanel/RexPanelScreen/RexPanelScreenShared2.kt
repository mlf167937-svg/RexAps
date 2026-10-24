package com.rexaps.rexpanel

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
internal fun ConnectForm(
    state: ConnState,
    onConnect: (
        String,
        String,
        Boolean
    ) -> Unit,
    onExit: () -> Unit
) {
    val context =
        LocalContext.current

    val prefs =
        remember {
            context.getSharedPreferences(
                "rexpanel",
                Context.MODE_PRIVATE
            )
        }

    var command by rememberSaveable(
        stateSaver = TextFieldValue.Saver
    ) {
        val t =
            prefs
                .getString(
                    "last_cmd",
                    ""
                )
                .orEmpty()

        mutableStateOf(
            TextFieldValue(
                t,
                TextRange(t.length)
            )
        )
    }

    var password by remember {
        mutableStateOf("")
    }

    var showPw by remember {
        mutableStateOf(false)
    }

    var elevate by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                "elevate",
                false
            )
        )
    }

    val busy =
        state is ConnState.Connecting

    val accent =
        Brush.linearGradient(
            listOf(
                colors.primary,
                colors.tertiary
            )
        )

    fun preset(t: String) {
        command =
            TextFieldValue(
                t,
                TextRange(t.length)
            )
    }

    fun submit() {
        if (busy) return

        prefs
            .edit()
            .putString(
                "last_cmd",
                command.text.trim()
            )
            .putBoolean(
                "elevate",
                elevate
            )
            .apply()

        onConnect(
            command.text,
            password,
            elevate
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 24.dp,
                vertical = 12.dp
            )
    ) {

        RexPanelScreenConnectHeader(onExit = onExit, accent = accent, onPreset = ::preset)
        RexPanelScreenConnectFields(
            state = state,
            command = command,
            onCommandChange = { command = it },
            password = password,
            onPasswordChange = { password = it },
            showPw = showPw,
            onShowPwChange = { showPw = it },
            elevate = elevate,
            onElevateChange = { elevate = it },
            onSubmit = ::submit
        )
        RexPanelScreenConnectFooter(busy = busy, accent = accent, onSubmit = ::submit)
    }
}
