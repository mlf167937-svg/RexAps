//EditorArea.kt
package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.rexaps.rexcoder.model.RexCoderState

/** Horizontal = groups berdampingan (landscape/tablet), else bertumpuk (portrait). Dividers are draggable. */
@Composable
fun EditorArea(s: RexCoderState, horizontal: Boolean, modifier: Modifier = Modifier) {
    val groups = s.groups.toList()
    var areaPx by remember { mutableFloatStateOf(0f) }
    val handlePx = with(LocalDensity.current) { HANDLE_THICKNESS.toPx() }
    val availablePx = (areaPx - handlePx * (groups.size - 1)).coerceAtLeast(1f)
    val sized = modifier.onSizeChanged { areaPx = (if (horizontal) it.width else it.height).toFloat() }

    if (horizontal) Row(sized) {
        groups.forEachIndexed { i, g ->
            key(g.id) {
                if (i > 0) ResizeHandle(
                    vertical = true, desc = "Ubah lebar editor ${i} dan ${i + 1}",
                    onDelta = { s.resizeGroups(i - 1, it, availablePx) }, onReset = { s.equalizeGroups() }
                )
                EditorGroupView(s, g, i, Modifier.weight(g.weight.coerceAtLeast(0.01f)).fillMaxHeight())
            }
        }
    } else Column(sized) {
        groups.forEachIndexed { i, g ->
            key(g.id) {
                if (i > 0) ResizeHandle(
                    vertical = false, desc = "Ubah tinggi editor ${i} dan ${i + 1}",
                    onDelta = { s.resizeGroups(i - 1, it, availablePx) }, onReset = { s.equalizeGroups() }
                )
                EditorGroupView(s, g, i, Modifier.weight(g.weight.coerceAtLeast(0.01f)).fillMaxWidth())
            }
        }
    }
}
