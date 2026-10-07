package com.rexaps.rexcoder.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.rexaps.librex.rexcode.RexCode
import com.rexaps.librex.rexcode.RexTokenType
import com.rexaps.librex.rexcode.detection.RexLanguageDetector
import com.rexaps.rexcoder.theme.Rex

/** RexCoder delegates syntax intelligence to the shared RexCode engine. */
class RexCodeSyntaxTransformation(private val fileName: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlight(text.text, fileName), OffsetMapping.Identity)
}

private fun tokenColor(type: RexTokenType): Color = when (type) {
    RexTokenType.KEYWORD, RexTokenType.ANNOTATION, RexTokenType.DIRECTIVE -> Rex.SynKeyword
    RexTokenType.TYPE, RexTokenType.CLASS, RexTokenType.NAMESPACE -> Rex.SynType
    RexTokenType.FUNCTION -> Rex.SynFunction
    RexTokenType.STRING, RexTokenType.STRING_ESCAPE -> Rex.SynString
    RexTokenType.NUMBER -> Rex.SynNumber
    RexTokenType.BOOLEAN, RexTokenType.NULL -> Rex.SynControl
    RexTokenType.COMMENT, RexTokenType.DOC_COMMENT -> Rex.SynComment
    RexTokenType.VARIABLE, RexTokenType.PROPERTY, RexTokenType.ATTRIBUTE_NAME, RexTokenType.KEY -> Rex.SynVariable
    RexTokenType.HEADING -> Rex.SynKeyword
    else -> Rex.Text
}

fun highlight(src: String, fileName: String): AnnotatedString {
    val result = AnnotatedString.Builder(src)
    val language = RexLanguageDetector.detect(fileName)
    RexCode.tokenize(src, language).forEach { token ->
        if (token.start < token.end && token.end <= src.length) {
            result.addStyle(SpanStyle(color = tokenColor(token.type)), token.start, token.end)
        }
    }
    return result.toAnnotatedString()
}
