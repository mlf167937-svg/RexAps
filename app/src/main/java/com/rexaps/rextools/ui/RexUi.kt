// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ═════════════════════ TOKENS ═════════════════════ */

val RexSm = RoundedCornerShape(12.dp)
val RexMd = RoundedCornerShape(18.dp)
val RexLg = RoundedCornerShape(26.dp)

/** Easing "ease-out expo": cepat di awal, mendarat sangat halus. */
val RexEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun hairline(alpha: Float = 0.7f) = __splitImpl_hairline(alpha)

/* ═════════════════════ THEME (opsional) ═════════════════════ */

@Composable
fun RexTheme(content: @Composable () -> Unit) {
        return __splitImpl_RexTheme(content)
    }

/* ═════════════════════ MODIFIERS ═════════════════════ */

/** Skala menyusut halus saat ditekan (pegas). */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier = this.__splitImpl_pressScale(source, pressedScale)

/** Muncul berurutan: fade + naik + sedikit membesar. */
fun Modifier.entrance(index: Int = 0, delayPer: Int = 70, offset: Dp = 22.dp): Modifier = this.__splitImpl_entrance(index, delayPer, offset)

/** Kilau bergerak untuk placeholder loading. */
fun Modifier.shimmer(): Modifier = this.__splitImpl_shimmer()

/* ═════════════════════ BACKGROUND & SCREEN ═════════════════════ */

/** Latar: dua cahaya aksen yang melayang pelan + grid tipis. Animasi dibaca di fase draw (tanpa recomposition). */
@Composable
fun RexBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
        return __splitImpl_RexBackground(modifier, content)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexScreen(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    scroll: Boolean = true,
    padded: Boolean = true,
    toast: RexToastState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
        return __splitImpl_RexScreen(title, onBack, subtitle, scroll, padded, toast, content)
    }

@Composable
fun RexBackButton(onClick: () -> Unit) {
        return __splitImpl_RexBackButton(onClick)
    }

/* ═════════════════════ PANEL ═════════════════════ */

@Composable
fun RexPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RexLg,
    padding: Dp = 16.dp,
    spacing: Dp = 16.dp,
    glow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
        return __splitImpl_RexPanel(modifier, onClick, shape, padding, spacing, glow, content)
    }

/* ═════════════════════ ICON TILE & HERO ═════════════════════ */

@Composable
fun RexIconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 48.dp, pulse: Boolean = false) {
        return __splitImpl_RexIconTile(icon, modifier, size, pulse)
    }

@Composable
fun RexHero(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
        return __splitImpl_RexHero(icon, title, description, modifier)
    }

@Composable
fun RexOverline(text: String, modifier: Modifier = Modifier) {
        return __splitImpl_RexOverline(text, modifier)
    }

/* ═════════════════════ INPUT ═════════════════════ */

@Composable
fun RexField(
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
        return __splitImpl_RexField(value, onChange, label, modifier, placeholder, icon, clearable, shape, keyboardOptions, keyboardActions)
    }

/* ═════════════════════ BUTTONS ═════════════════════ */

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
        return __splitImpl_RexPrimaryButton(text, icon, onClick, modifier, loading, loadingText, enabled)
    }

@Composable
internal fun RexButtonBase(
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
        return __splitImpl_RexButtonBase(text, icon, onClick, modifier, container, content, border, enabled, loading)
    }

@Composable
fun RexTonalButton(
    text: String, icon: ImageVector?, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false
) {
        return __splitImpl_RexTonalButton(text, icon, onClick, modifier, enabled, loading)
    }

@Composable
fun RexOutlinedButton(
    text: String, icon: ImageVector?, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true
) {
        return __splitImpl_RexOutlinedButton(text, icon, onClick, modifier, enabled)
    }

@Composable
fun RexRoundButton(
    icon: ImageVector, contentDescription: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = 52.dp
) {
        return __splitImpl_RexRoundButton(icon, contentDescription, onClick, modifier, size)
    }

@Composable
fun RexQuickAction(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_RexQuickAction(text, icon, onClick, modifier)
    }

/* ═════════════════════ PILL, CHIP, STAT ═════════════════════ */

@Composable
fun RexPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    content: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
        return __splitImpl_RexPill(text, modifier, container, content, icon)
    }

@Composable
fun RexFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
        return __splitImpl_RexFilterChip(label, selected, onClick)
    }

/** Angka murni dihitung naik (count-up); teks seperti "1.2M" tampil apa adanya. */
@Composable
fun RexStat(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, index: Int = 0) {
        return __splitImpl_RexStat(label, value, modifier, icon, index)
    }

/* ═════════════════════ FEEDBACK ═════════════════════ */

@Composable
fun RexErrorSlot(error: String?, title: String) {
        return __splitImpl_RexErrorSlot(error, title)
    }

@Composable
fun RexErrorBanner(title: String, message: String) {
        return __splitImpl_RexErrorBanner(title, message)
    }

@Composable
fun RexEmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
        return __splitImpl_RexEmptyState(icon, title, message, modifier)
    }

/* ── Toast ── */

@Stable
class RexToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    var tick by mutableStateOf(0)
        private set

    fun show(text: String, error: Boolean = false) {
        message = text
        isError = error
        tick++
    }

    fun dismiss() { message = null }
}

@Composable
fun rememberRexToast(): RexToastState = remember { RexToastState() }

@Composable
fun RexToastHost(state: RexToastState, modifier: Modifier = Modifier) {
        return __splitImpl_RexToastHost(state, modifier)
    }

/* ═════════════════════ LOADING / PHASE ═════════════════════ */

enum class RexPhase { Idle, Loading, Result, Hidden }

/** Pindah mulus antara kondisi kosong, loading (skeleton), dan hasil. */
@Composable
fun RexPhaseSwitch(
    phase: RexPhase,
    modifier: Modifier = Modifier,
    idle: @Composable () -> Unit,
    loading: @Composable () -> Unit,
    result: @Composable () -> Unit
) {
        return __splitImpl_RexPhaseSwitch(phase, modifier, idle, loading, result)
    }

@Composable
fun RexSkeleton(modifier: Modifier = Modifier, shape: Shape = RexSm) {
        return __splitImpl_RexSkeleton(modifier, shape)
    }

@Composable
fun RexSkeletonPanel(media: Boolean = true, modifier: Modifier = Modifier) {
        return __splitImpl_RexSkeletonPanel(media, modifier)
    }

/* ═════════════════════ MEDIA ═════════════════════ */

@Composable
fun RexImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
        return __splitImpl_RexImage(model, contentDescription, modifier, contentScale)
    }

/** Avatar dengan cincin gradien yang berputar pelan. */
@Composable
fun RexAvatar(
    model: Any?,
    contentDescription: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Dp = 3.dp
) {
        return __splitImpl_RexAvatar(model, contentDescription, size, modifier, ring)
    }

@Composable
fun RexEqualizer(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
        return __splitImpl_RexEqualizer(modifier, color)
    }
