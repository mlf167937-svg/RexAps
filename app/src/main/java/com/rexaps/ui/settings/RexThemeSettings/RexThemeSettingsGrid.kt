package com.rexaps.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.ui.theme.*

@Composable
internal fun RexThemeSettingsGrid(
    filtered: List<RexDesignTokens>,
    theme: RexDesignTokens,
    animationsEnabled: Boolean,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier,
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
                    ) { onSelect(item.id) }
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
                    onClick = { onDetail(item.id) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 1.dp)
                ) { Text("Lihat detail", fontSize = item.fontCaption.sp, fontFamily = rexFontFamily(item.fontFamilyUI)) }
            }
        }
    }
}
