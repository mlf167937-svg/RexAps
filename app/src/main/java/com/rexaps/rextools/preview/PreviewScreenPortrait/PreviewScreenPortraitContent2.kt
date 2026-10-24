package com.rexaps.rextools.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rextools.ui.*

@Composable
internal fun __splitImpl_InputView(
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

@Composable
internal fun __splitImpl_GridTile(item: PreviewItem, number: Int, index: Int, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val desc = when (item.type) {
        MediaType.IMAGE -> "Gambar $number"
        MediaType.VIDEO -> "Video $number"
        MediaType.UNKNOWN -> "Link $number"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .entrance(index.coerceAtMost(12), 45, 18.dp)
            .pressScale(interaction, 0.95f)
            .clip(RexMd)
            .background(s.surfaceContainerHigh)
            .border(hairline(), RexMd)
            .semantics { contentDescription = desc }
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    ) {
        if (item.type == MediaType.UNKNOWN) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RexIconTile(Icons.Default.Link, size = 48.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = s.onSurfaceVariant
                )
            }
        } else {
            RexImage(item.url, null, Modifier.fillMaxSize(), ContentScale.Crop)

            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
            )

            if (item.type == MediaType.VIDEO) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Text(
                item.fileName,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val (ic, label) = when (item.type) {
                MediaType.IMAGE -> Icons.Default.Image to "IMG"
                MediaType.VIDEO -> Icons.Default.Videocam to "VID"
                MediaType.UNKNOWN -> Icons.Default.Link to "URL"
            }
            Icon(ic, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
