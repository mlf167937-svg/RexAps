package com.rexaps.rextools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenPadding = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexToolsScreen(
    onToolClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RexToolsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "header") {
                RexToolsHeader(totalTools = totalTools)
            }
            item(key = "search") {
                ToolSearchField(
                    query = query,
                    onChange = { query = it },
                    onClear = { query = "" },
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 4.dp)
                )
            }
            item(key = "filters") {
                CategoryFilterRow(
                    categories = uiState.categories,
                    selected = uiState.selectedCategory,
                    onSelect = { viewModel.selectCategory(it) }
                )
            }

            if (sections.isEmpty()) {
                item(key = "empty") { ToolsEmptyState(query) }
            } else {
                item(key = "count") {
                    Text(
                        "$shownCount tool",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = ScreenPadding,
                            vertical = 4.dp
                        )
                    )
                }
                sections.forEach { (category, tools) ->
                    item(key = "h-${category.name}") {
                        SectionLabel(category.displayName, tools.size)
                    }
                    items(tools, key = { it.id }) { tool ->
                        RexToolCard(
                            tool = tool,
                            onClick = { onToolClick(tool.route) },
                            modifier = Modifier.padding(
                                horizontal = ScreenPadding,
                                vertical = 5.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── Header ─────────────────────────

@Composable
private fun RexToolsHeader(totalTools: Int) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        scheme.primaryContainer.copy(alpha = 0.55f),
                        scheme.tertiaryContainer.copy(alpha = 0.25f)
                    )
                )
            )
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
                Text(
                    "RexTools",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Koleksi tools serba bisa untuk aktivitas digital kamu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = CircleShape,
                    color = scheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        "$totalTools tools tersedia",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ───────────────────────── Search ─────────────────────────

@Composable
private fun ToolSearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text("Cari tool") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Cari") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Hapus pencarian")
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.7f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = scheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth()
    )
}

// ───────────────────────── Filters ─────────────────────────

@Composable
private fun CategoryFilterRow(
    categories: List<ToolCategory>,
    selected: ToolCategory?,
    onSelect: (ToolCategory?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            FilterPill("Semua", selected == null) { onSelect(null) }
        }
        items(categories, key = { it.name }) { category ->
            FilterPill(category.displayName, selected == category) {
                onSelect(if (selected == category) null else category)
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.6f),
        label = "pillContainer"
    )
    val content by animateColorAsState(
        if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        label = "pillContent"
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = if (selected) null else BorderStroke(1.dp, scheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier
                .heightIn(min = 20.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// ───────────────────────── Section label ─────────────────────────

@Composable
private fun SectionLabel(title: String, count: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(CircleShape)
                .background(scheme.primary)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Surface(shape = CircleShape, color = scheme.secondaryContainer) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ───────────────────────── Tool card ─────────────────────────

@Composable
private fun RexToolCard(
    tool: RexTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "toolPress"
    )
    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(22.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolIconBadge(tool.icon)
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
                        StatusPill("Baru", scheme.tertiaryContainer, scheme.onTertiaryContainer)
                    }
                    if (tool.isBeta) {
                        StatusPill("Beta", scheme.secondaryContainer, scheme.onSecondaryContainer)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ToolIconBadge(icon: ImageVector) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(scheme.primaryContainer, scheme.tertiaryContainer)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun StatusPill(text: String, container: Color, content: Color) {
    Surface(
        modifier = Modifier.padding(start = 6.dp),
        shape = CircleShape,
        color = container
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ───────────────────────── Empty state ─────────────────────────

@Composable
private fun ToolsEmptyState(query: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Tool tidak ditemukan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (query.isBlank()) "Belum ada tool di kategori ini."
            else "Tidak ada hasil untuk \"$query\". Coba kata kunci lain.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
