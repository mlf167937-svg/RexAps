package com.rexaps.rextools

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

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
            // Invisible top bar — header is drawn inside LazyColumn for scroll-away effect
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(scheme.surfaceVariant.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Kembali",
                                modifier = Modifier.size(20.dp),
                                tint = scheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {

            // ── Hero Header ──────────────────────────────────────────────────
            item(key = "header") {
                RexToolsHeader(totalTools = totalTools)
            }

            // ── Search ───────────────────────────────────────────────────────
            item(key = "search") {
                ToolSearchField(
                    query = query,
                    onChange = { query = it },
                    onClear = { query = "" },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // ── Category Filters ─────────────────────────────────────────────
            item(key = "filters") {
                CategoryFilterRow(
                    categories = uiState.categories,
                    selected = uiState.selectedCategory,
                    onSelect = { viewModel.selectCategory(it) }
                )
            }

            // ── Result Count ─────────────────────────────────────────────────
            if (sections.isNotEmpty()) {
                item(key = "count") {
                    Text(
                        "$shownCount tool ditemukan",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp,
                            top = 16.dp, bottom = 4.dp
                        )
                    )
                }
            }

            // ── Sections ─────────────────────────────────────────────────────
            if (sections.isEmpty()) {
                item(key = "empty") { ToolsEmptyState(query) }
            } else {
                sections.forEach { (category, tools) ->
                    item(key = "h-${category.name}") {
                        SectionLabel(category.displayName, tools.size)
                    }
                    items(tools, key = { it.id }) { tool ->
                        RexToolCard(
                            tool = tool,
                            onClick = { onToolClick(tool.route) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RexToolsHeader(totalTools: Int) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val tertiary = scheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.15f),
                        tertiary.copy(alpha = 0.08f),
                        scheme.background
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.MAX_VALUE, Float.MAX_VALUE)
                )
            )
            .drawBehind {
                // Subtle glowing orb top-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(80f, 60f),
                        radius = 180f
                    ),
                    radius = 180f,
                    center = Offset(80f, 60f)
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "RexTools",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Koleksi tools serba bisa\nuntuk aktivitas digital kamu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(14.dp))
                // Tool count badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        "$totalTools Tools Tersedia",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            // Decorative icon stack
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(scheme.primaryContainer, scheme.tertiaryContainer)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚡",
                    fontSize = 32.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search Field
// ─────────────────────────────────────────────────────────────────────────────

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
        placeholder = {
            Text(
                "Cari tool...",
                color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Cari",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "Hapus",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
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
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Filter Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryFilterRow(
    categories: List<ToolCategory>,
    selected: ToolCategory?,
    onSelect: (ToolCategory?) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            PremiumFilterChip(
                label = "Semua",
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(categories, key = { it.name }) { category ->
            PremiumFilterChip(
                label = category.displayName,
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) }
            )
        }
    }
}

@Composable
private fun PremiumFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = CircleShape,
        color = if (selected) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (selected) null else BorderStroke(1.dp, scheme.outline.copy(alpha = 0.2f)),
        tonalElevation = if (selected) 0.dp else 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, count: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primary, scheme.tertiary)
                    )
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = scheme.secondaryContainer.copy(alpha = 0.7f)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tool Card
// ─────────────────────────────────────────────────────────────────────────────

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
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "toolPress"
    )

    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(22.dp),
        color = scheme.surface,
        shadowElevation = if (pressed) 2.dp else 4.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    scheme.outline.copy(alpha = 0.18f),
                    scheme.outline.copy(alpha = 0.06f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            ToolIconBadge(tool.icon)
            Spacer(Modifier.width(16.dp))

            // Text content
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        tool.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tool.isNew) StatusPill(
                        text = "Baru",
                        containerColor = scheme.tertiaryContainer,
                        contentColor = scheme.onTertiaryContainer
                    )
                    if (tool.isBeta) StatusPill(
                        text = "Beta",
                        containerColor = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Arrow
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tool Icon Badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToolIconBadge(icon: ImageVector) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        scheme.primaryContainer,
                        scheme.tertiaryContainer.copy(alpha = 0.8f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.MAX_VALUE, Float.MAX_VALUE)
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

// ─────────────────────────────────────────────────────────────────────────────
// Status Pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = CircleShape,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToolsEmptyState(query: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon container with gradient ring
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            scheme.primary.copy(alpha = 0.14f),
                            scheme.primary.copy(alpha = 0.04f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Tidak ditemukan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (query.isBlank()) "Belum ada tool di kategori ini."
            else "Tidak ada hasil untuk \"$query\".\nCoba kata kunci lain.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
