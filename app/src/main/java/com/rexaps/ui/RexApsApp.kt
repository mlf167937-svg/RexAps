package com.rexaps.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rexaps.rexfox.RexFoxScreen
import com.rexaps.rexfox.RexFoxTheme
import kotlinx.coroutines.delay

private data class BottomTab(val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab("Home", Icons.Default.Home),
    BottomTab("Profile", Icons.Default.Person),
    BottomTab("Settings", Icons.Default.Settings)
)

private const val THEME_COLUMNS = 3

@Composable
fun RexApsApp(activity: Activity) {
    var rexFoxOpen by remember {
        mutableStateOf(false)
    }

    // Tema dibaca dari penyimpanan saat aplikasi dibuka, dan disimpan tiap kali diganti.
    val themeStore = remember { RexThemeStore(activity) }

    var themeOption by remember {
        mutableStateOf(themeStore.load())
    }

    val onThemeChange: (RexThemeOption) -> Unit = { option ->
        themeOption = option
        themeStore.save(option)
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

            AnimatedContent(
                targetState = selectedTab.intValue,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1

                    (fadeIn(tween(300, delayMillis = 80)) +
                        slideInHorizontally(
                            animationSpec = tween(360, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> fullWidth / 10 * direction }
                        )) togetherWith
                        (fadeOut(tween(160)) +
                            slideOutHorizontally(
                                animationSpec = tween(360, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -fullWidth / 10 * direction }
                            ))
                },
                label = "tabTransition"
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
                        onThemeSelected = onThemeChange
                    )
                }
            }
        }

        if (showThemeSheet) {
            ThemeSheet(
                selected = themeOption,
                onSelect = onThemeChange,
                onDismiss = { showThemeSheet = false }
            )
        }
    }
}

/* -------------------------------- ANIMATION ------------------------------- */

/** Elemen muncul bergantian: fade + naik sedikit. Hanya 8 item pertama yang diberi jeda. */
@Composable
private fun Modifier.entrance(index: Int): Modifier {
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(8) * 55L)
        shown = true
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "entrance"
    )

    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 40.dp.toPx()
    }
}

@Composable
private fun accentBrush(): Brush = Brush.linearGradient(
    listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    )
)

/* ---------------------------------- HOME ---------------------------------- */

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
            HomeHeader(
                onOpenTheme = onOpenTheme,
                modifier = Modifier.entrance(0)
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroCard(
                appCount = apps.size,
                modifier = Modifier.entrance(1)
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .entrance(2)
            )
        }

        itemsIndexed(apps, key = { _, app -> app.id }) { index, app ->
            AppCard(
                app = app,
                modifier = Modifier.entrance(index + 3),
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
private fun HomeHeader(
    onOpenTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
private fun HeroCard(
    appCount: Int,
    modifier: Modifier = Modifier
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Card(
        onClick = onClick,
        enabled = app.available,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
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
                .entrance(0)
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
            textAlign = TextAlign.Center,
            modifier = Modifier.entrance(1)
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
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.entrance(0)
            )
        }

        item {
            Column(modifier = Modifier.entrance(1)) {
                SectionLabel("Tema")
                Text(
                    text = "Aktif: ${selectedTheme.label}. Pilihanmu tersimpan otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ThemePicker(
                selected = selectedTheme,
                onSelect = onThemeSelected,
                modifier = Modifier.entrance(2)
            )
        }

        item {
            SectionLabel("Tentang")
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.entrance(3)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
    onSelect: (RexThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RexThemeOption.values().toList().chunked(THEME_COLUMNS).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { option ->
                    ThemeTile(
                        option = option,
                        isSelected = option == selected,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(THEME_COLUMNS - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(
    option: RexThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val preview = option.colorScheme(isSystemInDarkTheme())
    val shape = RoundedCornerShape(20.dp)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.outlineVariant,
        animationSpec = tween(250),
        label = "tileBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.97f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(preview.background)
                .border(1.dp, preview.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            Brush.linearGradient(listOf(preview.primary, preview.tertiary)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(10.dp)
                        .background(preview.surfaceVariant, RoundedCornerShape(50))
                        .border(1.dp, preview.outlineVariant, RoundedCornerShape(50))
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Dipilih",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        )
    }
}
