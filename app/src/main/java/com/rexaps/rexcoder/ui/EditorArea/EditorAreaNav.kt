package com.rexaps.rexcoder.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.model.DocState
import com.rexaps.rexcoder.model.EditorGroup
import com.rexaps.rexcoder.model.RexCoderState
import com.rexaps.rexcoder.theme.Rex

@Composable
internal fun TabBar(s: RexCoderState, g: EditorGroup, index: Int, focused: Boolean) {
    val listState = rememberLazyListState()
    LaunchedEffect(g.active) {
        val i = g.tabs.indexOf(g.active)
        if (i >= 0) listState.animateScrollToItem(i)
    }
    Row(Modifier.fillMaxWidth().height(36.dp).background(Rex.TabBar), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(Modifier.weight(1f), state = listState) {
            items(g.tabs.toList(), key = { it }) { path ->
                s.docs[path]?.let { Tab(s, g, it, active = path == g.active, groupFocused = focused) }
            }
        }
        FontSizeChip(label = "E${index + 1}", group = g, focused = focused) { s.setFontSize(g, it) }
        if (s.groups.size < 3) SmallIconButton(Icons.Outlined.ViewColumn, "Split editor") {
            s.focusedGroupId = g.id; s.splitEditor()
        }
        if (s.groups.size > 1) SmallIconButton(Icons.Outlined.Close, "Close editor group") { s.removeGroup(g.id) }
    }
}

@Composable
internal fun Tab(s: RexCoderState, g: EditorGroup, doc: DocState, active: Boolean, groupFocused: Boolean) {
    val (icon, tint) = fileStyle(doc.name)
    Row(
        Modifier
            .fillMaxHeight()
            .background(if (active) Rex.EditorBg else Rex.TabInactive)
            .drawBehind {
                if (active) {
                    if (groupFocused) drawRect(Rex.accentBrush(), size = Size(size.width, 2.dp.toPx()))
                    else drawRect(Rex.TextDim.copy(alpha = 0.5f), size = Size(size.width, 2.dp.toPx()))
                }
                drawRect(Rex.Border, Offset(size.width - 1.dp.toPx(), 0f), Size(1.dp.toPx(), size.height))
            }
            .clickable { g.active = doc.path; s.focusedGroupId = g.id }
            .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            doc.name, fontSize = 13.sp, maxLines = 1,
            color = if (active) Rex.TextBright else Rex.TextDim,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
        Box(
            Modifier.size(32.dp).clip(CircleShape).clickable { s.closeTab(g, doc.path) },
            contentAlignment = Alignment.Center
        ) {
            if (doc.modified) Box(Modifier.size(8.dp).clip(CircleShape).background(Rex.Modified))
            else Icon(Icons.Outlined.Close, "Close ${doc.name}", tint = Rex.TextDim, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
internal fun Breadcrumbs(path: String) {
    val parts = path.substringAfter("/Download/RexAps/", path).split('/').filter { it.isNotEmpty() }
    Row(
        Modifier.fillMaxWidth().height(24.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        parts.forEachIndexed { i, p ->
            if (i > 0) Icon(Icons.Outlined.ChevronRight, null, tint = Rex.TextDim, modifier = Modifier.size(14.dp))
            Text(p, fontSize = 12.sp, maxLines = 1, color = if (i == parts.lastIndex) Rex.Text else Rex.TextDim)
        }
    }
}
