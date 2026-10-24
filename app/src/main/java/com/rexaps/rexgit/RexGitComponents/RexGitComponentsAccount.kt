@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Person

// ============================================================
// GitHub accounts
// ============================================================

/** Small chips to switch between the connected accounts. */
@Composable
internal fun AccountSwitcher(
    accounts: List<String>,
    active: Int,
    onSwitch: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        accounts.forEachIndexed { index, name ->
            val selected = index == active

            Surface(
                onClick = { onSwitch(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
                contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (selected) scheme.primary.copy(alpha = 0.4f) else scheme.outlineVariant
                )
            ) {
                Row(
                    Modifier
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selected) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "@$name",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccountRow(
    name: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (active) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (active) scheme.primary.copy(alpha = 0.45f) else scheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .heightIn(min = 64.dp)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "@$name",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    if (active) "In use" else "Tap to use this account",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) scheme.primary else scheme.onSurfaceVariant
                )
            }

            if (active) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "In use",
                    tint = scheme.primary
                )
            }

            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove @$name",
                    tint = scheme.error.copy(alpha = if (enabled) 1f else 0.38f)
                )
            }
        }
    }
}

@Composable
internal fun AccountForm(
    loading: Boolean,
    submitLabel: String,
    onSubmit: (username: String, token: String) -> Unit,
    onCancel: (() -> Unit)?
) {
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("GitHub username") },
            singleLine = true,
            enabled = !loading,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrect = false,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Personal access token") },
            singleLine = true,
            enabled = !loading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                imeAction = ImeAction.Done
            ),
            visualTransformation =
                if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (visible) "Hide token" else "Show token"
                    )
                }
            }
        )

        Text(
            "Create a token at github.com/settings/tokens with the \"repo\" scope. " +
                "Never share it in chat or commit it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCancel != null) {
                TextButton(onClick = onCancel, enabled = !loading) { Text("Cancel") }
            }

            Button(
                onClick = { onSubmit(username, token) },
                enabled = !loading && username.isNotBlank() && token.isNotBlank()
            ) {
                if (loading) {
                    ButtonSpinner()
                    Spacer(Modifier.width(8.dp))
                }

                Text(if (loading) "Connecting" else submitLabel)
            }
        }
    }
}
