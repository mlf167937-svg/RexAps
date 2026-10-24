package com.rexaps.rexcoder.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.*

enum class SideView(val title: String, val icon: ImageVector) {
    Explorer("Explorer", Icons.Outlined.Folder),
    Search("Search", Icons.Outlined.Search),
    Git("Source Control", Icons.Outlined.AccountTree),
    Extensions("Extensions", Icons.Outlined.Extension)
}

/** Smallest share (of the total weight) an editor group may shrink to while dragging a divider. */
const val MIN_GROUP_SHARE = 0.18f
