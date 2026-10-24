package com.rexaps.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.ui.theme.*

@Composable
internal fun RexThemeSettingsFilters(
    query: String,
    category: String,
    categories: List<String>,
    theme: RexDesignTokens,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit
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
        onValueChange = onQueryChange,
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
}
