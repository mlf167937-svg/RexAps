package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import kotlinx.coroutines.delay

@Composable
internal fun __splitImpl_RexField(
    value: String,
    onChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    icon: ImageVector? = null,
    clearable: Boolean = true,
    shape: Shape = RexMd,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val s = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val glow by animateDpAsState(if (focused) 16.dp else 0.dp, tween(320), label = "glow")
    val trailing: (@Composable () -> Unit)? = if (clearable) {
        {
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f)
            ) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus teks")
                }
            }
        }
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailing,
        singleLine = true,
        shape = shape,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = source,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = s.surfaceContainerLow,
            unfocusedContainerColor = s.surfaceContainerLow,
            focusedBorderColor = s.primary,
            unfocusedBorderColor = s.outlineVariant,
            focusedLeadingIconColor = s.primary,
            unfocusedLeadingIconColor = s.onSurfaceVariant,
            focusedLabelColor = s.primary,
            cursorColor = s.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .shadow(glow, shape, ambientColor = s.primary, spotColor = s.primary)
    )
}

@Composable
internal fun __splitImpl_RexButtonBase(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier,
    container: Color,
    content: Color,
    border: BorderStroke?,
    enabled: Boolean,
    loading: Boolean
) {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val alphaAnim by animateFloatAsState(if (enabled) 1f else 0.4f, label = "a")
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && !loading,
        modifier = modifier.height(50.dp).pressScale(source).graphicsLayer { this.alpha = alphaAnim },
        shape = RexMd,
        color = container,
        contentColor = content,
        border = border,
        interactionSource = source
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "btnBase"
            ) { isLoading ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    } else {
                        if (icon != null) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_RexToastHost(state: RexToastState, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    LaunchedEffect(state.tick) {
        if (state.message != null) {
            delay(2600)
            state.dismiss()
        }
    }
    var last by remember { mutableStateOf("") }
    SideEffect { state.message?.let { last = it } }
    val accent = if (state.isError) s.error else s.secondary

    AnimatedVisibility(
        visible = state.message != null,
        modifier = modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        enter = slideInVertically(spring(0.7f, Spring.StiffnessMediumLow)) { it * 2 } + fadeIn() + scaleIn(initialScale = 0.85f),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)) + scaleOut(targetScale = 0.9f)
    ) {
        Row(
            Modifier
                .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
                .clip(CircleShape)
                .background(s.surfaceContainerHighest)
                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (state.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)
            )
            Text(state.message ?: last, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 2)
        }
    }
}

/** Angka murni dihitung naik (count-up); teks seperti "1.2M" tampil apa adanya. */
@Composable
internal fun __splitImpl_RexStat(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, index: Int = 0) {
    val s = MaterialTheme.colorScheme
    val numeric = remember(value) { if (value.isNotEmpty() && value.all { it.isDigit() }) value.toLongOrNull() else null }
    val fraction = remember(value) { Animatable(0f) }
    LaunchedEffect(value) {
        if (numeric != null) fraction.animateTo(1f, tween(1100, delayMillis = index * 80, easing = RexEase))
    }
    val shown = when {
        numeric == null -> value
        fraction.value >= 1f -> NumberFormat.getInstance().format(numeric)
        else -> NumberFormat.getInstance().format((numeric * fraction.value.toDouble()).toLong())
    }
    Column(
        modifier
            .entrance(index, 60, 14.dp)
            .clip(RexMd)
            .background(Brush.verticalGradient(listOf(s.surfaceContainerHigh.copy(alpha = 0.85f), s.surfaceContainer.copy(alpha = 0.6f))))
            .border(1.dp, s.outlineVariant.copy(alpha = 0.5f), RexMd)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(18.dp))
        Text(shown, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall, color = s.onSurfaceVariant, maxLines = 1)
    }
}
