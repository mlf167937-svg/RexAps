// RexToolsScreen.kt
package com.rexaps.rextools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rextools.ui.*

private val ScreenPadding = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexToolsScreenPortrait(
    onToolClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RexToolsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val sections = remember(uiState.selectedCategory, uiState.categories, query) {
        val q = query.trim()
        val cats = uiState.selectedCategory?.let { listOf(it) } ?: uiState.categories
        cats.map { cat ->
            cat to RexToolRegistry.toolsFor(cat).filter {
                q.isEmpty() ||
                    it.name.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true)
            }
        }.filter { it.second.isNotEmpty() }
    }
    val totalTools = remember { RexToolRegistry.allTools.size }
    val shownCount = sections.sumOf { it.second.size }

    // Header memudar & bergeser saat di-scroll (dibaca di fase draw, hemat recomposition)
    val headerProgress = remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 360f).coerceIn(0f, 1f)
        }
    }

    RexBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = { RexBackButton(onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                item(key = "header") {
                    Box(
                        Modifier.graphicsLayer {
                            alpha = 1f - headerProgress.value * 0.9f
                            translationY = headerProgress.value * 40f
                        }
                    ) { ToolsHeader(totalTools) }
                }
                item(key = "search") {
                    RexField(
                        value = query,
                        onChange = { query = it },
                        label = null,
                        placeholder = "Cari tool…",
                        icon = Icons.Default.Search,
                        shape = CircleShape,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .padding(horizontal = ScreenPadding, vertical = 6.dp)
                            .entrance(1)
                    )
                }
                item(key = "filters") {
                    LazyRow(
                        modifier = Modifier.entrance(2),
                        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "all") {
                            RexFilterChip("Semua", uiState.selectedCategory == null) { viewModel.selectCategory(null) }
                        }
                        items(uiState.categories, key = { it.name }) { category ->
                            val selected = uiState.selectedCategory == category
                            RexFilterChip(category.displayName, selected) {
                                viewModel.selectCategory(if (selected) null else category)
                            }
                        }
                    }
                }

                if (sections.isEmpty()) {
                    item(key = "empty") {
                        RexEmptyState(
                            icon = Icons.Default.Search,
                            title = "Tool tidak ditemukan",
                            message = if (query.isBlank()) "Belum ada tool di kategori ini."
                            else "Tidak ada hasil untuk \"$query\". Coba kata kunci lain."
                        )
                    }
                } else {
                    item(key = "count") {
                        AnimatedContent(
                            targetState = shownCount,
                            transitionSpec = {
                                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                            },
                            modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 4.dp),
                            label = "count"
                        ) { n -> RexOverline("$n tool") }
                    }
                    sections.forEach { (category, tools) ->
                        item(key = "h-${category.name}") { SectionLabel(category.displayName, tools.size) }
                        itemsIndexed(tools, key = { _, t -> t.id }) { i, tool ->
                            ToolCard(
                                tool = tool,
                                onClick = { onToolClick(tool.route) },
                                modifier = Modifier
                                    .padding(horizontal = ScreenPadding, vertical = 5.dp)
                                    .entrance(index = i.coerceAtMost(7), delayPer = 50)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ToolsHeader(totalTools: Int) {
    val s = MaterialTheme.colorScheme
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(totalTools) { target = totalTools }
    val shown by animateIntAsState(target, tween(1000), label = "count")

    Column(
        Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 8.dp).entrance(0),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RexPill("$shown tools tersedia", icon = Icons.Default.Bolt)
        Text(
            "RexTools",
            style = MaterialTheme.typography.displaySmall.copy(
                brush = Brush.linearGradient(listOf(s.onSurface, s.primary, s.tertiary))
            )
        )
        Text(
            "Koleksi tools serba bisa untuk aktivitas digital kamu.",
            style = MaterialTheme.typography.bodyMedium,
            color = s.onSurfaceVariant,
            lineHeight = 21.sp
        )
    }
}

@Composable
internal fun SectionLabel(title: String, count: Int) {
    val s = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.width(3.dp).height(14.dp)
                .background(Brush.verticalGradient(listOf(s.primary, s.tertiary)), CircleShape)
        )
        RexOverline(title)
        Box(Modifier.weight(1f).height(1.dp).background(s.outlineVariant.copy(alpha = 0.5f)))
        Text(
            "%02d".format(count),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = s.onSurfaceVariant
        )
    }
}

@Composable
internal fun ToolCard(tool: RexTool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    RexPanel(modifier = modifier, onClick = onClick, shape = RexMd, padding = 14.dp, spacing = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RexIconTile(tool.icon, size = 50.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tool.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tool.isNew) {
                        RexPill("Baru", Modifier.padding(start = 6.dp), s.tertiaryContainer, s.onTertiaryContainer)
                    }
                    if (tool.isBeta) {
                        RexPill("Beta", Modifier.padding(start = 6.dp), s.secondaryContainer, s.onSecondaryContainer)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = s.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(32.dp).background(s.primary.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null, tint = s.primary, modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
