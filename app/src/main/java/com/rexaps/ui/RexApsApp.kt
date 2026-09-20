package com.rexaps.ui

import android.app.Activity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

/* ---------------------------------- ROOT ---------------------------------- */

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
        FadeInScreen {
            RexFoxTheme {
                RexFoxScreen(activity = activity)
            }
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
        val colors = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // Cahaya lembut di bagian atas layar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.primary.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                contentColor = colors.onBackground,
                bottomBar = {
                    FloatingNavBar(
                        selected = selectedTab.intValue,
                        onSelect = { selectedTab.intValue = it }
                    )
                }
            ) { padding ->

                Crossfade(
                    targetState = selectedTab.intValue,
                    animationSpec = tween(350),
                    label = "tab"
                ) { tab ->
                    when (tab) {
                        0 -> HomeContent(
                            padding = padding,
                            themeLabel = themeOption.label,
                            onAppClick = { app ->
                                if (app.id == "rexfox" && app.available) {
                                    rexFoxOpen = true
                                }
                            },
                            onOpenTheme = {
                                showThemeSheet = true
                            }
                        )

                        1 -> ProfileContent(
                            padding = padding,
                            appCount = RexAppRegistry.modules.size,
                            themeLabel = themeOption.label
                        )

                        else -> SettingsContent(
                            padding = padding,
                            selectedTheme = themeOption,
                            onThemeSelected = onThemeChange
                        )
                    }
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

/* ------------------------------ ANIMATION KIT ----------------------------- */

/** Layar RexFox muncul dengan fade halus. */
@Composable
private fun FadeInScreen(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "screenFade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        content()
    }
}

/** Elemen muncul bergantian: fade + naik + zoom kecil. */
@Composable
private fun Modifier.entrance(index: Int): Modifier {
    var shown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index.coerceAtMost(8) * 60L)
        }
        shown = true
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "entrance"
    )

    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 48.dp.toPx()
        val s = 0.94f + 0.06f * progress
        scaleX = s
        scaleY = s
    }
}

/** Klik dengan efek mengecil memantul saat ditekan. */
@Composable
private fun Modifier.pressable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

@Composable
private fun accentBrush(): Brush = Brush.linearGradient(
    listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    )
)

/* ------------------------------- NAVIGATION ------------------------------- */

@Composable
private fun FloatingNavBar(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.surfaceVariant,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomTabs.forEachIndexed { index, tab ->
                    NavItem(
                        tab = tab,
                        selected = index == selected,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = if (selected) colors.primary else Color.Transparent,
        animationSpec = tween(300),
        label = "navBg"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = tween(300),
        label = "navFg"
    )

    Row(
        modifier = Modifier
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clip(CircleShape)
            .background(background)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = foreground
        )

        if (selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = foreground,
                maxLines = 1
            )
        }
    }
}

/* ---------------------------------- HOME ---------------------------------- */

@Composable
private fun HomeContent(
    padding: PaddingValues,
    themeLabel: String,
    onAppClick: (RexModule) -> Unit,
    onOpenTheme: () -> Unit
) {
    val apps = RexAppRegistry.modules
    val featured = apps.firstOrNull { it.id == "rexfox" }
    val others = apps.filter { it.id != "rexfox" }

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
                themeLabel = themeLabel,
                modifier = Modifier.entrance(1)
            )
        }

        if (featured != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCard(
                    app = featured,
                    onClick = { onAppClick(featured) },
                    modifier = Modifier.entrance(2)
                )
            }
        }

        if (others.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .entrance(3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aplikasi lainnya",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${others.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        itemsIndexed(others, key = { _, app -> app.id }) { index, app ->
            AppCard(
                app = app,
                onClick = { onAppClick(app) },
                modifier = Modifier.entrance(index + 4)
            )
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Selamat datang di",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
            Text(
                text = "RexAps",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .pressable(onClick = onOpenTheme)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outlineVariant, CircleShape)
                .semantics { contentDescription = "Ubah tema" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(accentBrush(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colors.surfaceVariant, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    appCount: Int,
    themeLabel: String,
    modifier: Modifier = Modifier
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary

    val transition = rememberInfiniteTransition(label = "hero")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(accentBrush())
            .drawBehind {
                drawCircle(
                    color = onAccent.copy(alpha = 0.12f),
                    radius = 110.dp.toPx(),
                    center = Offset(
                        x = size.width - 30.dp.toPx() - drift * 30.dp.toPx(),
                        y = 10.dp.toPx() + drift * 24.dp.toPx()
                    )
                )
                drawCircle(
                    color = onAccent.copy(alpha = 0.09f),
                    radius = 64.dp.toPx(),
                    center = Offset(
                        x = size.width - 90.dp.toPx() + drift * 36.dp.toPx(),
                        y = size.height - 10.dp.toPx() - drift * 20.dp.toPx()
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(onAccent.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$appCount aplikasi",
                    style = MaterialTheme.typography.labelMedium,
                    color = onAccent
                )
            }

            Text(
                text = "Semua aplikasimu,\nsatu tempat.",
                style = MaterialTheme.typography.headlineSmall,
                color = onAccent
            )

            Text(
                text = "Tema aktif: $themeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = onAccent.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AppIcon(
    letter: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.32f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = 0.5f),
                spotColor = colors.primary.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(accentBrush()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onPrimary
        )
    }
}

@Composable
private fun StatusPill(available: Boolean) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (available) colors.primaryContainer
                else colors.outlineVariant.copy(alpha = 0.5f)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    if (available) colors.primary else colors.onSurfaceVariant,
                    CircleShape
                )
        )
        Text(
            text = if (available) "Tersedia" else "Segera",
            style = MaterialTheme.typography.labelSmall,
            color = if (available) colors.onPrimaryContainer else colors.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturedCard(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    val primary = colors.primary
    val outline = colors.outlineVariant

    val transition = rememberInfiniteTransition(label = "featured")
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressable(enabled = app.available, onClick = onClick)
            .alpha(if (app.available) 1f else 0.6f)
            .clip(shape)
            .background(colors.surfaceVariant)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.28f * glow), Color.Transparent),
                        center = Offset(size.width * 0.9f, 0f),
                        radius = size.width * 0.75f
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.9f, 0f)
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.3f + 0.6f * glow), outline)
                ),
                shape = shape
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AppIcon(letter = app.name.take(1).uppercase(), size = 64.dp)
                StatusPill(available = app.available)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (app.available) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentBrush())
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Buka",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                }
            }
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
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .pressable(enabled = app.available, onClick = onClick)
            .alpha(if (app.available) 1f else 0.55f)
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AppIcon(letter = app.name.take(1).uppercase(), size = 48.dp)

                if (app.available) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(colors.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPrimaryContainer
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.outlineVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Segera",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* --------------------------------- PROFILE -------------------------------- */

@Composable
private fun ProfileContent(
    padding: PaddingValues,
    appCount: Int,
    themeLabel: String
) {
    val colors = MaterialTheme.colorScheme

    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "angle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .entrance(0)
                .size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = angle }
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(colors.primary, colors.tertiary, colors.primary)
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .background(colors.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .background(accentBrush(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Profile RexAps",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.entrance(1)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Aplikasi",
                value = "$appCount",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Tema",
                value = themeLabel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    val colors = MaterialTheme.colorScheme

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
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .entrance(1)
            ) {
                Text(
                    text = "Tema",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Aktif: ${selectedTheme.label}. Pilihanmu tersimpan otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
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
            val shape = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .entrance(3)
                    .clip(shape)
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.outlineVariant, shape)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIcon(letter = "R", size = 48.dp)

                Column {
                    Text(
                        text = "RexAps",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Semua aplikasimu, satu tempat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/* ---------------------------------- THEME --------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSheet(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
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
    val previewShape = RoundedCornerShape(14.dp)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.outlineVariant,
        animationSpec = tween(250),
        label = "tileBorder"
    )
    val tileScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileScale"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
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
                .height(72.dp)
                .clip(previewShape)
                .background(preview.background)
                .border(1.dp, preview.outlineVariant, previewShape)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(8.dp)
                        .background(
                            Brush.linearGradient(listOf(preview.primary, preview.tertiary)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(10.dp)
                        .background(preview.surfaceVariant, CircleShape)
                        .border(1.dp, preview.outlineVariant, CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkScale.coerceIn(0f, 1f)
                    }
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
