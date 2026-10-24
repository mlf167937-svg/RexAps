package com.rexaps.rexpanel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun RexPanelScreenConnectFields(
    state: ConnState,
    command: TextFieldValue,
    onCommandChange: (TextFieldValue) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPw: Boolean,
    onShowPwChange: (Boolean) -> Unit,
    elevate: Boolean,
    onElevateChange: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
        OutlinedTextField(
            value = command,
            onValueChange = {
                onCommandChange(it)
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
                onPasswordChange(it)
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
                        onShowPwChange(!showPw)
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
                        onSubmit()
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
                        onElevateChange(it)
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
}
