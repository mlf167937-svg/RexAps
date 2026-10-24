package com.rexaps.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.ui.theme.*

@Composable
internal fun RexThemeSettingsMotionControls(
    theme: RexDesignTokens,
    settings: RexMotionSettings,
    onSettingsChange: (RexMotionSettings) -> Unit,
    onCancelPreview: () -> Unit,
    onCommitPreview: () -> Unit,
    onReset: () -> Unit
) {
    Spacer(Modifier.height(10.dp))
    Text("Tingkat animasi", color = Color(theme.text), fontWeight = rexFontWeight(theme.weightSemibold), fontSize = theme.fontHeading.sp, fontFamily = rexFontFamily(theme.fontFamilyUI))
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        RexAnimationLevel.values().forEach { level ->
            FilterChip(
                selected = settings.level == level,
                onClick = { onSettingsChange(settings.copy(level = level)) },
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
        onValueChange = { onSettingsChange(settings.copy(effectIntensity = it.toInt())) },
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
            onCheckedChange = { onSettingsChange(settings.copy(followSystemReduceMotion = it)) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(theme.onPrimary), checkedTrackColor = Color(theme.primary),
                uncheckedThumbColor = Color(theme.textMuted), uncheckedTrackColor = Color(theme.surfaceElevated)
            )
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onCancelPreview() },
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(theme.radiusMd.dp),
            border = BorderStroke(theme.borderThin.dp, Color(theme.border)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(theme.text))
        ) { Text("Batal", fontFamily = rexFontFamily(theme.fontFamilyUI)) }
        Button(
            onClick = { onCommitPreview() },
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(theme.radiusMd.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(theme.primary), contentColor = Color(theme.onPrimary))
        ) { Text("Terapkan", fontFamily = rexFontFamily(theme.fontFamilyUI)) }
        TextButton(
            onClick = { onReset() },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(theme.primary))
        ) { Text("Reset", fontFamily = rexFontFamily(theme.fontFamilyUI)) }
    }
}
