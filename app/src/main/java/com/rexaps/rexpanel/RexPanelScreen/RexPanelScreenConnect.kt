package com.rexaps.rexpanel

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexfox.rexPressable

/* -------------------------------- CONNECT -------------------------------- */

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
    val colors =
        MaterialTheme.colorScheme

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

        IconButton(
            onClick = onExit
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "Kembali"
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        Box(
            Modifier
                .size(64.dp)
                .clip(
                    RoundedCornerShape(20.dp)
                )
                .background(accent),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                ">_",
                color = colors.onPrimary,
                fontWeight =
                    FontWeight.Bold,
                fontFamily =
                    FontFamily.Monospace,
                fontSize = 22.sp
            )
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "RexPanel",
            style =
                MaterialTheme.typography
                    .headlineLarge
        )

        Text(
            "Monitor, terminal, dan file server lewat SSH. Tidak perlu root.",
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                colors.onSurfaceVariant
        )

        Spacer(
            Modifier.height(24.dp)
        )

        Row(
            Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            PresetChip(
                "Termux · 8022"
            ) {
                preset(
                    "ssh -p 8022 user@192.168."
                )
            }

            PresetChip(
                "Server Linux · 22"
            ) {
                preset(
                    "ssh user@"
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = command,
            onValueChange = {
                command = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text("Perintah SSH")
            },
            placeholder = {
                Text(
                    "ssh -p 8022 u0_a123@192.168.0.101"
                )
            },
            supportingText = {
                Text(
                    "User wajib ditulis. Di Termux biasanya u0_aXXX (cek dengan whoami)."
                )
            },
            singleLine = true,
            shape =
                RoundedCornerShape(20.dp),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Uri,
                    imeAction =
                        ImeAction.Next
                )
        )

        Spacer(
            Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            singleLine = true,
            visualTransformation =
                if (showPw) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {

                IconButton(
                    onClick = {
                        showPw = !showPw
                    }
                ) {
                    Icon(
                        if (showPw) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        if (showPw) {
                            "Sembunyikan password"
                        } else {
                            "Tampilkan password"
                        }
                    )
                }
            },
            shape =
                RoundedCornerShape(20.dp),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password,
                    imeAction =
                        ImeAction.Go
                ),
            keyboardActions =
                KeyboardActions(
                    onGo = {
                        submit()
                    }
                )
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .toggleable(
                    value = elevate,
                    role = Role.Switch,
                    onValueChange = {
                        elevate = it
                    }
                )
                .padding(
                    vertical = 8.dp,
                    horizontal = 4.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    "Izinkan sudo/su",
                    style =
                        MaterialTheme.typography
                            .bodyLarge
                )

                Text(
                    "Opsional. Hanya untuk membaca data sistem yang butuh izin lebih. Biarkan mati untuk akun biasa.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        colors.onSurfaceVariant
                )
            }

            Spacer(
                Modifier.width(12.dp)
            )

            Switch(
                checked = elevate,
                onCheckedChange = null
            )
        }

        if (state is ConnState.Error) {

            Spacer(
                Modifier.height(8.dp)
            )

            Row(
                Modifier.semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                },
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = colors.error,
                    modifier =
                        Modifier.size(18.dp)
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    state.message,
                    color = colors.error,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }

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
                    submit()
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
}
