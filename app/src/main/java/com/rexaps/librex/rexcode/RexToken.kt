package com.rexaps.librex.rexcode

data class RexToken(
    val type: RexTokenType,
    val text: String,
    val start: Int,
    val end: Int,
    val line: Int,
    val column: Int
)
