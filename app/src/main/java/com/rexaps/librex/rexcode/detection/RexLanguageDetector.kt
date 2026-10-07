package com.rexaps.librex.rexcode.detection

import com.rexaps.librex.rexcode.*

object RexLanguageDetector {
    fun detect(fileName: String): RexLanguage {
        return RexLanguageRegistry.findByFileName(fileName).language
    }
}
