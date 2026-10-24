package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

data class SearchHit(val path: String, val name: String, val line: Int, val text: String)

fun RexCoderState.__split_search_14(q: String): List<SearchHit> {
        if (q.length < 2) return emptyList()
        return root.flatFiles().asSequence().flatMap { f ->
            val text = docs[f.path]?.value?.text ?: f.content
            text.lineSequence().mapIndexedNotNull { i, l -> if (l.contains(q, true)) SearchHit(f.path, f.name, i + 1, l.trim()) else null }
        }.take(200).toList()
    }
