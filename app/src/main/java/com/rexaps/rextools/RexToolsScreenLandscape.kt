package com.rexaps.rextools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import com.rexaps.rextools.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexToolsScreenLandscape(
    onToolClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RexToolsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember(uiState.selectedCategory, uiState.categories, query) {
        val q = query.trim()
        val base = uiState.selectedCategory?.let { category -> RexToolRegistry.toolsFor(category) } ?: RexToolRegistry.allTools
        base.filter { q.isEmpty() || it.name.contains(q, true) || it.description.contains(q, true) }
    }

    RexBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = { TopAppBar(title = { Text("RexTools", style = MaterialTheme.typography.titleLarge) }, navigationIcon = { RexBackButton(onBack) }) }
        ) { inner ->
            Row(
                Modifier.fillMaxSize().padding(inner).padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    Modifier.width(292.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RexPanel(spacing = 10.dp) {
                        RexPill("${RexToolRegistry.allTools.size} tools tersedia", icon = Icons.Default.Bolt)
                        Text("RexTools", style = MaterialTheme.typography.headlineLarge)
                        Text("Koleksi tools serba bisa untuk aktivitas digital kamu.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    RexField(
                        value = query, onChange = { query = it }, label = null,
                        placeholder = "Cari tool…", icon = Icons.Default.Search,
                        shape = CircleShape, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                    RexOverline("Kategori")
                    RexFilterChip("Semua", uiState.selectedCategory == null) { viewModel.selectCategory(null) }
                    uiState.categories.forEach { category ->
                        RexFilterChip(category.displayName, uiState.selectedCategory == category) {
                            viewModel.selectCategory(if (uiState.selectedCategory == category) null else category)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    RexOverline("${all.size} tool ditampilkan")
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = all.isEmpty(),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tools-grid-state"
                    ) { empty ->
                        if (empty) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                RexEmptyState(Icons.Default.Search, "Tool tidak ditemukan", "Coba kata kunci atau kategori lain.")
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 260.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(all, key = { it.id }) { tool ->
                                    ToolCard(tool = tool, onClick = { onToolClick(tool.route) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
