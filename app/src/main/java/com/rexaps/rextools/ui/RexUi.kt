// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ───────────── Tokens ───────────── */

val RexSm = RoundedCornerShape(12.dp)
val RexMd = RoundedCornerShape(16.dp)
val RexLg = RoundedCornerShape(24.dp)

@Composable
fun hairline(alpha: Float = 0.6f) =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))

/* ───────────── Background & Screen ───────────── */

/** Latar dengan cahaya aksen yang sangat halus di pojok kanan atas. */
@Composable
fun RexBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val s = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(s.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(s.primary.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.95f
                    )
                )
            },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexScreen(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    scroll: Boolean = true,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = MaterialTheme.colorScheme
    RexBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (subtitle != null) {
                                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = s.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = { RexBackButton(onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { pad ->
            var m = Modifier.fillMaxSize().padding(pad)
            if (scroll) m = m.verticalScroll(rememberScrollState())
            if (padded) m = m.padding(horizontal = 20.dp, vertical = 8.dp)
            Column(
                modifier = m,
                verticalArrangement = if (padded) Arrangement.spacedBy(16.dp) else Arrangement.Top,
                content = content
            )
        }
    }
}

@Composable
fun RexBackButton(onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.padding(start = 8.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
        )
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
    }
}

/* ───────────── Panel ───────────── */

@Composable
fun RexPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RexLg,
    padding: Dp = 16.dp,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = MaterialTheme.colorScheme
    val color = s.surfaceContainerLow.copy(alpha = 0.85f)
    val inner: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
    if (onClick == null) {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = hairline(), content = inner)
    } else {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
        Surface(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale },
            shape = shape,
            color = color,
            border = hairline(),
            interactionSource = src,
            content = inner
        )
    }
}

/* ───────────── Icon tile & hero ───────────── */

@Composable
fun RexIconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val s = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(RexSm)
            .background(s.primary.copy(alpha = 0.12f))
            .border(1.dp, s.primary.copy(alpha = 0.28f), RexSm),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun RexHero(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        RexIconTile(icon, size = 52.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RexOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/* ───────────── Inputs ───────────── */

@Composable
fun RexField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    icon: ImageVector? = null,
    clearable: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val s = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = if (icon != null) { { Icon(icon, contentDescription = null) } } else null,
        trailingIcon = if (clearable && value.isNotEmpty()) {
            { IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, contentDescription = "Hapus teks") } }
        } else null,
        singleLine = true,
        shape = RexMd,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = s.surfaceContainerLow,
            unfocusedContainerColor = s.surfaceContainerLow.copy(alpha = 0.7f),
            focusedBorderColor = s.primary,
            unfocusedBorderColor = s.outlineVariant
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/* ───────────── Buttons ───────────── */

@Composable
private fun BtnContent(icon: ImageVector?, text: String) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

@Composable
fun RexPrimaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    loadingText: String = "Memproses…",
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RexMd,
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(loadingText, fontWeight = FontWeight.SemiBold)
        } else {
            BtnContent(icon, text)
        }
    }
}

@Composable
fun RexTonalButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, shape = RexMd, modifier = modifier.height(50.dp)) { BtnContent(icon, text) }
}

@Composable
fun RexOutlinedButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RexMd,
        border = hairline(0.9f),
        modifier = modifier.height(50.dp)
    ) { BtnContent(icon, text) }
}

/* ───────────── Feedback ───────────── */

@Composable
fun RexErrorSlot(error: String?, title: String) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        error?.let { RexErrorBanner(title, it) }
    }
}

@Composable
fun RexErrorBanner(title: String, message: String) {
    val s = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RexMd)
            .background(s.errorContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = s.onErrorContainer)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = s.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = s.onErrorContainer)
        }
    }
}

@Composable
fun RexEmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(s.surfaceContainerHigh)
                .border(1.dp, s.outlineVariant.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = s.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodySmall, color = s.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/* ───────────── Stat & Pill ───────────── */

@Composable
fun RexStat(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val s = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RexMd)
            .background(s.surfaceContainerHigh.copy(alpha = 0.7f))
            .border(1.dp, s.outlineVariant.copy(alpha = 0.4f), RexMd)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = s.primary, modifier = Modifier.size(18.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall, color = s.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun RexPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    content: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}

@Composable
fun RexFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) s.primary else Color.Transparent, label = "chipBg")
    val content by animateColorAsState(if (selected) s.onPrimary else s.onSurfaceVariant, label = "chipFg")
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = if (selected) null else hairline(0.9f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}