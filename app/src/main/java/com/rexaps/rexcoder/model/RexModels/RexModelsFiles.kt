package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

// Real filesystem tree. path is the absolute filesystem path.
data class FileNode(
    val path: String,
    val name: String,
    val isDir: Boolean,
    val children: List<FileNode> = emptyList(),
    val content: String = ""
)

fun FileNode.flatFiles(): List<FileNode> = if (!isDir) listOf(this) else children.flatMap { it.flatFiles() }
