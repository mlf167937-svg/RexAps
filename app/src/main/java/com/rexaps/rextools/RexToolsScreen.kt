// RexToolsScreen.kt
package com.rexaps.rextools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rextools.ui.*

private val ScreenPadding = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexToolsScreen(
    onToolClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RexToolsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

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

    RexBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = { RexBackButton(onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item(key = "header") { ToolsHeader(totalTools) }
                item(key = "search") {
                    SearchBar(
                        query = query,
                        onChange = { query = it },
                        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 4.dp)
                    )
                }
                item(key = "filters") {
                    LazyRow(
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
                        RexOverline(
                            "$shownCount tool",
                            Modifier.padding(horizontal = ScreenPadding, vertical = 4.dp)
                        )
                    }
                    sections.forEach { (category, tools) ->
                        item(key = "h-${category.name}") { SectionLabel(category.displayName, tools.size) }
                        items(tools, key = { it.id }) { tool ->
                            ToolCard(
                                tool = tool,
                                onClick = { onToolClick(tool.route) },
                                modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsHeader(totalTools: Int) {
    val s = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RexPill("$totalTools tools tersedia")
        Text(
            "RexTools",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
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
private fun SearchBar(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text("Cari tool") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                }
            }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = s.surfaceContainerHigh,
            unfocusedContainerColor = s.surfaceContainerLow,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            cursorColor = s.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionLabel(title: String, count: Int) {
    val s = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RexOverline(title)
        HorizontalDivider(Modifier.weight(1f), color = s.outlineVariant.copy(alpha = 0.5f))
        Text(
            "%02d".format(count),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = s.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolCard(tool: RexTool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    RexPanel(modifier = modifier, onClick = onClick, shape = RexMd, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RexIconTile(tool.icon, size = 48.dp)
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
                        RexPill(
                            "Baru",
                            Modifier.padding(start = 6.dp),
                            container = s.tertiaryContainer,
                            content = s.onTertiaryContainer
                        )
                    }
                    if (tool.isBeta) {
                        RexPill(
                            "Beta",
                            Modifier.padding(start = 6.dp),
                            container = s.secondaryContainer,
                            content = s.onSecondaryContainer
                        )
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
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = s.onSurfaceVariant)
        }
    }
}