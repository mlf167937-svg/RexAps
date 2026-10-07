package com.rexaps.librex.rexcode.theme

import com.rexaps.librex.rexcode.RexTokenType

data class RexSyntaxTheme(
    val colors: Map<RexTokenType, Long> = defaultColors
) {
    operator fun get(type: RexTokenType): Long = colors[type] ?: colors[RexTokenType.TEXT] ?: 0xFFFFFFFF
    companion object {
        val defaultColors = mapOf(
            RexTokenType.KEYWORD to 0xFFCF8BFF, RexTokenType.TYPE to 0xFF6CCBFF,
            RexTokenType.FUNCTION to 0xFFFFD580, RexTokenType.CLASS to 0xFF7FDBCA,
            RexTokenType.STRING to 0xFF9FE08F, RexTokenType.NUMBER to 0xFFFFB86C,
            RexTokenType.BOOLEAN to 0xFFFF8FA3, RexTokenType.NULL to 0xFFFF8FA3,
            RexTokenType.COMMENT to 0xFF7F8C98, RexTokenType.DOC_COMMENT to 0xFF8097A6,
            RexTokenType.OPERATOR to 0xFFE5E7EB, RexTokenType.PUNCTUATION to 0xFFE5E7EB,
            RexTokenType.DELIMITER to 0xFFE5E7EB, RexTokenType.CONSTANT to 0xFFFFC777,
            RexTokenType.TEXT to 0xFFE8EAED
        )
    }
}
