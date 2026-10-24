package com.rexaps.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.ui.theme.*

@Composable
internal fun RexThemeSettingsDetail(
    visible: Boolean,
    selectedId: String,
    theme: RexDesignTokens,
    animationsEnabled: Boolean
) {
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
}
