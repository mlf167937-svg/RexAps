package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                RexIconButton(
                    Icons.Default.Clear, "Hapus pencarian", onClear,
                    size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            disabledContainerColor = scheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = scheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = modifier.fillMaxWidth()
    )
}
