package com.rexaps.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.ui.theme.*

private fun rexFontFamily(token: String): FontFamily = when (token.lowercase()) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

private fun rexFontWeight(value: Int): FontWeight = when {
    value >= 800 -> FontWeight.ExtraBold
    value >= 700 -> FontWeight.Bold
    value >= 600 -> FontWeight.SemiBold
    value >= 500 -> FontWeight.Medium
    value <= 300 -> FontWeight.Light
    else -> FontWeight.Normal
}

@Composable
fun RexThemeSettings(manager: RexThemeManager, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var selectedId by remember { mutableStateOf(manager.getCurrentTheme().id) }
    var showDetail by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(manager.motionSettings) }
    val theme = manager.getCurrentTheme()
    val animationsEnabled = settings.level != RexAnimationLevel.OFF
    val backgroundTop by animateColorAsState(Color(theme.background), tween(if (animationsEnabled) theme.motionNormal else 0), label = "theme-settings-bg-top")
    val backgroundBottom by animateColorAsState(Color(theme.backgroundAlt), tween(if (animationsEnabled) theme.motionSlow else 0), label = "theme-settings-bg-bottom")
    val categories = listOf("Semua", "Glass", "Dark", "Light", "Neon", "Cosmic", "Elemental")
    val filtered = manager.getThemesByCategory().values.flatten().filter {
        (category == "Semua" || it.category.equals(category, true)) &&
            (it.name.contains(query, true) || it.category.contains(query, true))
    }
    Column(
        modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundTop, backgroundBottom)))
            .animateContentSize(tween(if (animationsEnabled) theme.motionSlow else 0, easing = FastOutSlowInEasing))
            .padding(16.dp)
    ) {
        Text(
            "Tema & tampilan",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = theme.fontTitle.sp, fontFamily = rexFontFamily(theme.fontFamilyUI)),
            color = Color(theme.text), fontWeight = rexFontWeight(theme.weightBold)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "30 tema • preview langsung • pengaturan tersimpan",
            color = Color(theme.textMuted),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = theme.fontBody.sp, fontFamily = rexFontFamily(theme.fontFamilyUI))
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(theme.radiusLg.dp),
            label = { Text("Cari tema", color = Color(theme.textMuted), fontSize = theme.fontCaption.sp, fontFamily = rexFontFamily(theme.fontFamilyUI)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(theme.text), fontSize = theme.fontBody.sp, fontFamily = rexFontFamily(theme.fontFamilyUI)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(theme.text), unfocusedTextColor = Color(theme.text),
                focusedBorderColor = Color(theme.primary), unfocusedBorderColor = Color(theme.border),
                focusedLabelColor = Color(theme.primary), unfocusedLabelColor = Color(theme.textMuted),
                cursorColor = Color(theme.accent)
            )
        )
        Spacer(Modifier.height(9.dp))
        // Keep the category controls on one scrollable row on narrow screens.
        // A fixed-width Row previously risked clipping the last categories on phones.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            categories.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c, fontSize = theme.fontCaption.sp, fontFamily = rexFontFamily(theme.fontFamilyUI)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(theme.surface), labelColor = Color(theme.textMuted),
                        selectedContainerColor = Color(theme.selection), selectedLabelColor = Color(theme.text)
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 14.dp, top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Tema tidak ditemukan",
                            color = Color(theme.text),
                            fontWeight = rexFontWeight(theme.weightSemibold),
                            fontSize = theme.fontHeading.sp,
                            fontFamily = rexFontFamily(theme.fontFamilyUI)
                        )
                        Text(
                            "Coba kata kunci atau kategori lain.",
                            color = Color(theme.textMuted),
                            fontSize = theme.fontBody.sp,
                            fontFamily = rexFontFamily(theme.fontFamilyUI)
                        )
                    }
                }
            }
            items(filtered, key = { it.id }) { item ->
                val active = selectedId == item.id
                val tileShape = RoundedCornerShape(item.radiusLg.dp)
                val tileInteraction = remember(item.id) { MutableInteractionSource() }
                val hovered by tileInteraction.collectIsHoveredAsState()
                val selectedBorder by animateColorAsState(
                    Color(if (active || hovered) item.primary else item.border),
                    tween(if (animationsEnabled) theme.motionFast else 0), label = "tile-border-${item.id}"
                )
                val tileElevation by animateDpAsState(
                    if (active || hovered) item.shadowMd.dp else item.shadowSm.dp,
                    tween(if (animationsEnabled) theme.motionFast else 0), label = "tile-elevation-${item.id}"
                )
                val tileSurface by animateColorAsState(
                    Color(item.surface), tween(if (animationsEnabled) theme.motionNormal else 0), label = "tile-surface-${item.id}"
                )
                val tileScale by animateFloatAsState(
                    when { active -> 1.018f; hovered -> 1.008f; else -> 1f },
                    animationSpec = if (animationsEnabled) spring(dampingRatio = .72f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow) else androidx.compose.animation.core.snap(),
                    label = "tile-scale-${item.id}"
                )
                Column(
                    Modifier.graphicsLayer { scaleX = tileScale; scaleY = tileScale }
                        .shadow(tileElevation, tileShape)
                        .clip(tileShape)
                        .background(tileSurface)
                        .border(if (active) 2.dp else item.borderThin.dp, selectedBorder, tileShape)
                        .clickable(
                            interactionSource = tileInteraction,
                            indication = LocalIndication.current
                        ) {
                            selectedId = item.id
                            manager.previewTheme(item.id)
                        }
                        .animateContentSize(tween(if (animationsEnabled) item.motionNormal else 0, easing = FastOutSlowInEasing))
                        .padding(12.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(58.dp)
                            .clip(RoundedCornerShape(item.radiusMd.dp))
                            .background(Brush.linearGradient(item.gradientPrimary.map { Color(it) }))
                    ) {
                        Row(
                            Modifier.align(Alignment.BottomStart).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(item.primary, item.secondary, item.accent).forEachIndexed { index, swatch ->
                                Box(
                                    Modifier.size(if (index == 0) 12.dp else 10.dp)
                                        .clip(RoundedCornerShape(item.radiusPill.dp))
                                        .background(Color(swatch))
                                        .border(.7.dp, Color(item.onPrimary).copy(alpha = .32f), RoundedCornerShape(item.radiusPill.dp))
                                )
                            }
                        }
                        if (item.id == RexThemeRegistry.default.id) {
                            Text(
                                "DEFAULT",
                                Modifier.align(Alignment.TopEnd).padding(6.dp)
                                    .clip(RoundedCornerShape(item.radiusPill.dp))
                                    .background(Color(item.background).copy(alpha = .76f)).padding(horizontal = 7.dp, vertical = 3.dp),
                                color = Color(item.text),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = (item.fontCaption - 1).coerceAtLeast(10).sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (active) {
                            Text(
                                "✓",
                                Modifier.align(Alignment.TopEnd).padding(6.dp)
                                    .clip(RoundedCornerShape(item.radiusPill.dp))
                                    .background(Color(item.primary)).padding(horizontal = 7.dp, vertical = 2.dp),
                                color = Color(item.onPrimary), fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        item.name, color = Color(item.text),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = item.fontHeading.sp, fontFamily = rexFontFamily(item.fontFamilyUI)),
                        fontWeight = rexFontWeight(item.weightSemibold)
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        item.category.uppercase(), color = Color(item.textMuted),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = item.fontCaption.sp, fontFamily = rexFontFamily(item.fontFamilyUI)),
                        letterSpacing = .7.sp
                    )
                    TextButton(
                        onClick = { selectedId = item.id; manager.previewTheme(item.id); showDetail = true },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 1.dp)
                    ) { Text("Lihat detail", fontSize = item.fontCaption.sp, fontFamily = rexFontFamily(item.fontFamilyUI)) }
                }
            }
        }

        AnimatedVisibility(
            visible = showDetail,
            enter = fadeIn(tween(if (animationsEnabled) theme.motionNormal else 0)) +
                expandVertically(tween(if (animationsEnabled) theme.motionSlow else 0, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = .985f, animationSpec = tween(if (animationsEnabled) theme.motionSlow else 0)),
            exit = fadeOut(tween(if (animationsEnabled) theme.motionFast else 0)) +
                shrinkVertically(tween(if (animationsEnabled) theme.motionNormal else 0)) + scaleOut(targetScale = .985f, animationSpec = tween(if (animationsEnabled) theme.motionFast else 0))
        ) {
            val detail = RexThemeRegistry.get(selectedId)
            Column(
                Modifier.fillMaxWidth().shadow(detail.shadowMd.dp, RoundedCornerShape(detail.radiusXl.dp))
                    .clip(RoundedCornerShape(detail.radiusXl.dp))
                    .background(Color(detail.surface))
                    .border(BorderStroke(detail.borderThin.dp, Color(detail.border)))
                    .animateContentSize(tween(if (animationsEnabled) detail.motionNormal else 0))
                    .padding(14.dp)
            ) {
                Text(detail.name, color = Color(detail.text), fontWeight = rexFontWeight(detail.weightBold), fontSize = detail.fontTitle.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Spacer(Modifier.height(5.dp))
                Text(
                    "Warna: primary #${detail.primary.toString(16).takeLast(6).uppercase()}, secondary #${detail.secondary.toString(16).takeLast(6).uppercase()}, accent #${detail.accent.toString(16).takeLast(6).uppercase()}",
                    color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI)
                )
                Spacer(Modifier.height(5.dp))
                Text("Karakter: ${detail.category.lowercase()} — ${detail.signature}.", color = Color(detail.text), fontSize = detail.fontBody.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Text("Bentuk & tipografi: radius ${detail.radiusMd}/${detail.radiusXl} dp, font ${detail.fontFamilyUI}, ritme easing tersendiri.", color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Text("Efek visual: blur tematik, glow terkontrol, gradient berlapis, noise ${detail.noiseIntensity}.", color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Text("Animasi background: ${detail.backgroundMotion}.", color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Text("Animasi transisi: ${detail.transitionMotion}.", color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Text("Animasi hover/fokus/tap: ${detail.interactionMotion}.", color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Text("Animasi loading: ${detail.loadingMotion}.", color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
                Text("Animasi notifikasi: ${detail.notificationMotion}.", color = Color(detail.textMuted), fontSize = detail.fontCaption.sp, fontFamily = rexFontFamily(detail.fontFamilyUI))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Tingkat animasi", color = Color(theme.text), fontWeight = rexFontWeight(theme.weightSemibold), fontSize = theme.fontHeading.sp, fontFamily = rexFontFamily(theme.fontFamilyUI))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            RexAnimationLevel.values().forEach { level ->
                FilterChip(
                    selected = settings.level == level,
                    onClick = { settings = settings.copy(level = level); manager.updateMotion(settings) },
                    label = {
                        Text(when (level) {
                            RexAnimationLevel.FULL -> "Penuh"
                            RexAnimationLevel.BALANCED -> "Seimbang"
                            RexAnimationLevel.SAVER -> "Hemat"
                            RexAnimationLevel.OFF -> "Mati"
                        }, fontSize = theme.fontCaption.sp, fontFamily = rexFontFamily(theme.fontFamilyUI))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(theme.surface), labelColor = Color(theme.textMuted),
                        selectedContainerColor = Color(theme.selection), selectedLabelColor = Color(theme.text)
                    )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Intensitas efek: ${settings.effectIntensity}%", color = Color(theme.text), fontSize = theme.fontBody.sp, fontFamily = rexFontFamily(theme.fontFamilyUI))
        Slider(
            value = settings.effectIntensity.toFloat(),
            onValueChange = { settings = settings.copy(effectIntensity = it.toInt()); manager.updateMotion(settings) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color(theme.primary), activeTrackColor = Color(theme.primary),
                inactiveTrackColor = Color(theme.border), activeTickColor = Color(theme.accent),
                inactiveTickColor = Color(theme.textMuted)
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ikuti reduce motion sistem", modifier = Modifier.weight(1f), color = Color(theme.text), fontSize = theme.fontBody.sp, fontFamily = rexFontFamily(theme.fontFamilyUI))
            Switch(
                checked = settings.followSystemReduceMotion,
                onCheckedChange = { settings = settings.copy(followSystemReduceMotion = it); manager.updateMotion(settings) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(theme.onPrimary), checkedTrackColor = Color(theme.primary),
                    uncheckedThumbColor = Color(theme.textMuted), uncheckedTrackColor = Color(theme.surfaceElevated)
                )
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { manager.cancelPreview(); selectedId = manager.getCurrentTheme().id },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(theme.radiusMd.dp),
                border = BorderStroke(theme.borderThin.dp, Color(theme.border)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(theme.text))
            ) { Text("Batal", fontFamily = rexFontFamily(theme.fontFamilyUI)) }
            Button(
                onClick = { manager.commitPreview(); selectedId = manager.getCurrentTheme().id },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(theme.radiusMd.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primary), contentColor = Color(theme.onPrimary))
            ) { Text("Terapkan", fontFamily = rexFontFamily(theme.fontFamilyUI)) }
            TextButton(
                onClick = {
                    manager.setTheme(RexThemeRegistry.default.id)
                    selectedId = RexThemeRegistry.default.id
                    settings = RexMotionSettings()
                    manager.updateMotion(settings)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(theme.primary))
            ) { Text("Reset", fontFamily = rexFontFamily(theme.fontFamilyUI)) }
        }
    }
}
