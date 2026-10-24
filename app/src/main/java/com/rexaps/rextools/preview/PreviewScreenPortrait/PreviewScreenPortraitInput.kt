package com.rexaps.rextools.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*

/* ═════════════════════ INPUT ═════════════════════ */

@Composable
internal fun InputView(
    input: String,
    error: String?,
    onInput: (String) -> Unit,
    onExtract: () -> Unit,
    onClear: () -> Unit
) {
    val s = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (focused) s.primary.copy(alpha = 0.8f) else s.outlineVariant, tween(300), label = "bd")
    val links = remember(input) { Regex("""https?://[^\s"'<>\\]+""").findAll(input).count() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RexHero(
            icon = Icons.Default.Link,
            title = "Ekstrak media dari mana saja",
            description = "URL tunggal, JSON, atau teks apa pun yang berisi link.",
            modifier = Modifier.entrance(0)
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .entrance(1)
                .clip(RexLg)
                .background(s.surfaceContainerLow.copy(alpha = 0.85f))
                .border(1.dp, borderColor, RexLg)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RexOverline("Input")
                Spacer(Modifier.width(10.dp))
                AnimatedVisibility(
                    visible = links > 0,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) { RexPill("$links link terdeteksi", icon = Icons.Default.Link) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let(onInput)
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tempel")
                }
            }

            TextField(
                value = input,
                onValueChange = onInput,
                placeholder = {
                    Text(
                        "https://...  atau  {\"result\":[...]}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxSize().onFocusChanged { focused = it.isFocused },
                minLines = 6,
                keyboardOptions = KeyboardOptions.Default,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        RexErrorSlot(error, "Tidak bisa mengekstrak")

        Row(Modifier.fillMaxWidth().entrance(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RexOutlinedButton("Bersihkan", null, onClear, Modifier.weight(1f), enabled = input.isNotBlank())
            RexPrimaryButton(
                text = "Extract & Preview",
                icon = Icons.Default.Bolt,
                onClick = onExtract,
                enabled = input.isNotBlank(),
                modifier = Modifier.weight(2f)
            )
        }
    }
}
