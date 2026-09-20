package com.rexaps.ui

import android.app.Activity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexfox.RexFoxScreen
import com.rexaps.rexfox.RexFoxTheme

private data class BottomTab(val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab("Home", Icons.Default.Home),
    BottomTab("Profile", Icons.Default.Person),
    BottomTab("Settings", Icons.Default.Settings)
)

@Composable
fun RexApsApp(activity: Activity) {
    var rexFoxOpen by remember {
        mutableStateOf(false)
    }

    var themeOption by rememberSaveable {
        mutableStateOf(RexThemeOption.SYSTEM)
    }

    // When RexFox is open, show it full screen
    if (rexFoxOpen) {
        RexFoxTheme {
            RexFoxScreen(activity = activity)
        }
        return
    }

    val selectedTab = remember {
        mutableIntStateOf(0)
    }

    var showThemeSheet by rememberSaveable {
        mutableStateOf(false)
    }

    RexTheme(option = themeOption) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomTabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab.intValue == index,
                            onClick = {
                                selectedTab.intValue = index
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(tab.label)
                            }
                        )
                    }
                }
            }
        ) { padding ->

            Crossfade(
                targetState = selectedTab.intValue,
                label = "tab"
            ) { tab ->
                when (tab) {
                    0 -> HomeContent(
                        padding = padding,
                        onOpenRexFox = {
                            rexFoxOpen = true
                        },
                        onOpenTheme = {
                            showThemeSheet = true
                        }
                    )

                    1 -> ProfileContent(padding)

                    else -> SettingsContent(
                        padding = padding,
                        selectedTheme = themeOption,
                        onThemeSelected = { themeOption = it }
                    )
                }
            }
        }

        if (showThemeSheet) {
            ThemeSheet(
                selected = themeOption,
                onSelect = { themeOption = it },
                onDismiss = { showThemeSheet = false }
            )
        }
    }
}

/* ---------------------------------- HOME ---------------------------------- */

@Composable
private fun accentBrush(): Brush = Brush.linearGradient(
    listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    )
)

@Composable
private fun HomeContent(
    padding: PaddingValues,
    onOpenRexFox: () -> Unit,
    onOpenTheme: () -> Unit
) {
    val apps = RexAppRegistry.modules

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHeader(onOpenTheme = onOpenTheme)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroCard(appCount = apps.size)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        items(apps, key = { it.id }) { app ->
            AppCard(
                app = app,
                onClick = {
                    if (app.id == "rexfox" && app.available) {
                        onOpenRexFox()
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeHeader(onOpenTheme: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Selamat datang di",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "RexAps",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        IconButton(
            onClick = onOpenTheme,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Ubah tema" },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(accentBrush(), CircleShape)
            )
        }
    }
}

@Composable
private fun HeroCard(appCount: Int) {
    val onAccent = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(accentBrush())
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = 230.dp, y = (-50).dp)
                .background(onAccent.copy(alpha = 0.10f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = 170.dp, y = 70.dp)
                .background(onAccent.copy(alpha = 0.08f), CircleShape)
        )

        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = onAccent.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "$appCount aplikasi",
                    style = MaterialTheme.typography.labelMedium,
                    color = onAccent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "Semua aplikasimu,\nsatu tempat.",
                style = MaterialTheme.typography.headlineSmall,
                color = onAccent
            )
        }
    }
}

@Composable
private fun AppCard(
    app: RexModule,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        enabled = app.available,
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant,
            disabledContainerColor = colors.surfaceVariant
        ),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (app.available) 1f else 0.55f)
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = accentBrush(),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name.take(1),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onPrimary
                    )
                }

                if (!app.available) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primaryContainer
                    ) {
                        Text(
                            text = "Segera",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

/* --------------------------------- PROFILE -------------------------------- */

@Composable
private fun ProfileContent(
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(accentBrush(), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "R",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Profile RexAps",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

/* -------------------------------- SETTINGS -------------------------------- */

@Composable
private fun SettingsContent(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onThemeSelected: (RexThemeOption) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        item {
            SectionLabel("Tampilan")
        }

        item {
            ThemePicker(
                selected = selectedTheme,
                onSelect = onThemeSelected
            )
        }

        item {
            SectionLabel("Tentang")
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "RexAps",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Semua aplikasimu, satu tempat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp)
    )
}

/* ---------------------------------- THEME --------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSheet(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "Tema",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Pilih tampilan yang paling nyaman untukmu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ThemePicker(selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun ThemePicker(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RexThemeOption.values().forEach { option ->
            ThemeOptionRow(
                option = option,
                isSelected = option == selected,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    option: RexThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) colors.primaryContainer else colors.surfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.outlineVariant,
                shape = shape
            )
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ThemeSwatch(option)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = option.caption,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Dipilih",
                tint = colors.primary
            )
        }
    }
}

@Composable
private fun ThemeSwatch(option: RexThemeOption) {
    val preview = option.colorScheme(isSystemInDarkTheme())

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(preview.background)
            .border(1.dp, preview.outlineVariant, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    Brush.linearGradient(listOf(preview.primary, preview.tertiary)),
                    CircleShape
                )
        )
    }
}
