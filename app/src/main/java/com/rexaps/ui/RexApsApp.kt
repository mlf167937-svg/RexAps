// /data/data/com.termux/files/home/RexAps/app/src/main/java/com/rexaps/ui/RexApsApp.kt
package com.rexaps.ui

import android.app.Activity

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.rexcoder.RexCoderScreen

import com.rexaps.rexfox.RexFoxScreen
import com.rexaps.rexnux.RexNuxScreen
import com.rexaps.rextools.navigation.RexNavGraph
import com.rexaps.rexmusic.RexMusicScreen
import com.rexaps.rexmanager.RexManagerScreen
import com.rexaps.rexgit.RexGit

import kotlinx.coroutines.delay

private data class BottomTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomTabs = listOf(
    BottomTab("Home", Icons.Default.Home),
    BottomTab("Profile", Icons.Default.Person),
    BottomTab("Settings", Icons.Default.Settings)
)

private const val THEME_COLUMNS = 4

/* ---------------------------------- ROOT ---------------------------------- */

@Composable
fun RexApsApp(activity: Activity) {

    var route by remember { mutableStateOf(RexRoute.NONE) }
    var routeLoading by remember { mutableStateOf(false) }

    val themeStore = remember { RexThemeStore(activity) }
    var themeOption by remember { mutableStateOf(themeStore.load()) }

    var themeApplying by remember { mutableStateOf(false) }
    var pendingTheme by remember { mutableStateOf<RexThemeOption?>(null) }

    val onThemeChange: (RexThemeOption) -> Unit = { option ->
        if (option != themeOption) {
            pendingTheme = option
            themeApplying = true
        }
    }

    /*
     * SUB-APP ROUTES (RexFox / RexPanel / RexChat / RexNux / RexWARP ...)
     */

    if (route != RexRoute.NONE) {

        val appName = route.displayName

        RexTheme(option = themeOption) {
            Crossfade(
                targetState = routeLoading,
                animationSpec = tween(320),
                label = "routeTransition"
            ) { isLoading ->

                if (isLoading) {

                    RexAppLoader(
                        appName = appName,
                        onFinished = {
                            routeLoading = false
                        }
                    )

                } else {

                    when (route) {

                        RexRoute.REXFOX ->
                            RexFoxScreen(
                                activity = activity
                            )

                        RexRoute.REXPANEL ->
                            com.rexaps.rexpanel.RexPanelScreen(
                                activity = activity,
                                onExit = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXCHAT ->
                            com.rexaps.rexchat.SessionListScreen(
                                onOpen = { session, method ->
                                    com.rexaps.rexchat.openRexChatSession(
                                        activity,
                                        session,
                                        method
                                    )
                                },
                                onExit = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXNUX ->
                            RexNuxScreen(
                                onExit = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXTOOLS ->
                            RexNavGraph(
                                onBack = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXMUSIC ->
                            RexMusicScreen(
                                onBack = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXMANAGER ->
                            RexManagerScreen(
                                onExit = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXGIT ->
                            RexGit()

                        RexRoute.REXCODER ->
                            com.rexaps.rexcoder.RexCoderScreen(
                                activity = activity,
                                onExit = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXMONITOR ->
                            com.rexaps.rexmonitor.RexMonitorScreen(
                                onExit = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.REXWARP ->
                            com.rexaps.rexwarp.RexWarpScreen(
                                onExit = {
                                    route = RexRoute.NONE
                                }
                            )

                        RexRoute.NONE -> Unit
                    }
                }
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.20f),
                                colors.tertiary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                contentColor = colors.onBackground,
                bottomBar = {
                    FloatingNavBar(
                        selected = selectedTab.intValue,
                        onSelect = {
                            selectedTab.intValue = it
                        }
                    )
                }
            ) { padding ->

                Crossfade(
                    targetState = selectedTab.intValue,
                    animationSpec = tween(320),
                    label = "tab"
                ) { tab ->

                    when (tab) {

                        0 -> HomeContent(
                            padding = padding,
                            themeLabel = themeOption.label,
                            isActive = selectedTab.intValue == 0,
                            onAppClick = { app ->

                                if (!app.available) {
                                    return@HomeContent
                                }

                                val target =
                                    RexRoute.fromModuleId(app.id)

                                if (target != null) {
                                    route = target
                                    routeLoading = true
                                }
                            },
                            onOpenTheme = {
                                showThemeSheet = true
                            }
                        )

                        1 -> ProfileContent(
                            padding = padding,
                            appCount = RexAppRegistry.modules.size,
                            themeLabel = themeOption.label,
                            isActive = selectedTab.intValue == 1
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
                onDismiss = {
                    showThemeSheet = false
                }
            )
        }

        AnimatedVisibility(
            visible = themeApplying,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {

            pendingTheme?.let { target ->

                RexThemeLoadingOverlay(
                    targetOption = target,
                    onFinished = {

                        themeOption = target
                        themeStore.save(target)

                        pendingTheme = null
                        themeApplying = false
                        showThemeSheet = false
                    }
                )
            }
        }
    }
}

/* ------------------------------ ANIMATION KIT ----------------------------- */

@Composable
private fun Modifier.entrance(index: Int): Modifier {

    var shown by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        if (!shown) {
            delay(index.coerceAtMost(8) * 60L)
        }

        shown = true
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            550,
            easing = FastOutSlowInEasing
        ),
        label = "entrance"
    )

    return this.graphicsLayer {

        alpha = progress

        translationY =
            (1f - progress) * 48.dp.toPx()

        val scale =
            0.94f + 0.06f * progress

        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun Modifier.pressable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {

    val source = remember {
        MutableInteractionSource()
    }

    val pressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue =
            if (pressed && enabled) 0.96f else 1f,
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
private fun accentBrush(): Brush =
    Brush.linearGradient(
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
            .padding(
                horizontal = 22.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {

        Surface(
            shape = RoundedCornerShape(36.dp),
            color = colors.surfaceVariant.copy(alpha = 0.96f),
            shadowElevation = 18.dp,
            tonalElevation = 4.dp,
            border = BorderStroke(
                1.dp,
                colors.outlineVariant
            )
        ) {

            Row(
                modifier = Modifier.padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                bottomTabs.forEachIndexed { index, tab ->

                    NavItem(
                        tab = tab,
                        selected = index == selected,
                        onClick = {
                            onSelect(index)
                        }
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
        targetValue =
            if (selected) colors.primary
            else Color.Transparent,
        animationSpec = tween(300),
        label = "navBg"
    )

    val foreground by animateColorAsState(
        targetValue =
            if (selected) colors.onPrimary
            else colors.onSurfaceVariant,
        animationSpec = tween(300),
        label = "navFg"
    )

    val iconScale by animateFloatAsState(
        targetValue =
            if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIconScale"
    )

    Row(
        modifier = Modifier
            .animateContentSize(
                spring(
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(CircleShape)
            .background(background)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(
                horizontal = 16.dp,
                vertical = 11.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = foreground,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            }
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
    isActive: Boolean,
    onAppClick: (RexModule) -> Unit,
    onOpenTheme: () -> Unit
) {
    val apps = RexAppRegistry.modules
    val featured = apps.firstOrNull { it.id == "rexfox" }
    val others = apps.filter { it.id != "rexfox" }
    val availableCount = apps.count { it.available }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var appFilter by rememberSaveable { mutableStateOf("Semua") }

    val filteredApps = others.filter { app ->
        val matchesQuery = app.name.contains(searchQuery.trim(), ignoreCase = true) ||
            app.description.contains(searchQuery.trim(), ignoreCase = true)
        val matchesFilter = when (appFilter) {
            "Tersedia" -> app.available
            "Segera" -> !app.available
            else -> true
        }
        matchesQuery && matchesFilter
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 36.dp
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
                availableCount = availableCount,
                themeLabel = themeLabel,
                isActive = isActive,
                modifier = Modifier.entrance(1)
            )
        }

        if (featured != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCard(
                    app = featured,
                    isActive = isActive,
                    onClick = { onAppClick(featured) },
                    modifier = Modifier.entrance(2)
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(3),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Cari aplikasi atau fitur...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari aplikasi",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Semua", "Tersedia", "Segera").forEach { filter ->
                    FilterChip(
                        selected = appFilter == filter,
                        onClick = { appFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .entrance(5),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (searchQuery.isBlank()) "Jelajahi aplikasi" else "Hasil pencarian",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = when (appFilter) {
                            "Tersedia" -> "Aplikasi yang bisa dibuka sekarang"
                            "Segera" -> "Aplikasi yang akan hadir"
                            else -> "Semua tools dalam satu tempat"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "${filteredApps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (filteredApps.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Aplikasi tidak ditemukan", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Coba kata kunci lain atau ubah filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            itemsIndexed(
                filteredApps,
                key = { _, app -> app.id }
            ) { index, app ->
                AppCard(
                    app = app,
                    onClick = { onAppClick(app) },
                    modifier = Modifier.entrance(index + 6)
                )
            }
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
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Column {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            colors.primary,
                            CircleShape
                        )
                )

                Text(
                    text = "Selamat datang di",
                    style =
                        MaterialTheme.typography.labelLarge,
                    color =
                        colors.onSurfaceVariant
                )
            }

            Text(
                text = "RexAps",
                style =
                    MaterialTheme.typography.headlineLarge
            )
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor =
                        colors.primary.copy(alpha = 0.4f),
                    spotColor =
                        colors.primary.copy(alpha = 0.4f)
                )
                .pressable(
                    onClick = onOpenTheme
                )
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .border(
                    1.dp,
                    colors.outlineVariant,
                    CircleShape
                )
                .semantics {
                    contentDescription = "Ubah tema"
                },
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        accentBrush(),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(
                            colors.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    appCount: Int,
    availableCount: Int,
    themeLabel: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {

    val onAccent =
        MaterialTheme.colorScheme.onPrimary

    val transition =
        rememberInfiniteTransition(
            label = "hero"
        )

    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue =
            if (isActive) 1f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    7000,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.35f
                    ),
                spotColor =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.35f
                    )
            )
            .clip(RoundedCornerShape(32.dp))
            .background(accentBrush())
            .drawBehind {

                drawCircle(
                    color = onAccent.copy(alpha = 0.14f),
                    radius = 120.dp.toPx(),
                    center = Offset(
                        x =
                            size.width -
                                30.dp.toPx() -
                                drift * 30.dp.toPx(),
                        y =
                            10.dp.toPx() +
                                drift * 24.dp.toPx()
                    )
                )

                drawCircle(
                    color = onAccent.copy(alpha = 0.10f),
                    radius = 70.dp.toPx(),
                    center = Offset(
                        x =
                            size.width -
                                90.dp.toPx() +
                                drift * 36.dp.toPx(),
                        y =
                            size.height -
                                10.dp.toPx() -
                                drift * 20.dp.toPx()
                    )
                )
            }
    ) {

        Column(
            modifier = Modifier.padding(26.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        onAccent.copy(alpha = 0.18f)
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
            ) {

                Text(
                    text = "$appCount aplikasi",
                    style =
                        MaterialTheme.typography.labelMedium,
                    color = onAccent
                )
            }

            Text(
                text = "Semua aplikasimu,\nsatu tempat.",
                style =
                    MaterialTheme.typography.headlineSmall,
                color = onAccent
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                HeroStatChip(
                    label = "Tersedia",
                    value = "$availableCount",
                    onAccent = onAccent
                )

                HeroStatChip(
                    label = "Tema aktif",
                    value = themeLabel,
                    onAccent = onAccent
                )
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    label: String,
    value: String,
    onAccent: Color
) {

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                onAccent.copy(alpha = 0.12f)
            )
            .border(
                1.dp,
                onAccent.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {

        Text(
            text = value,
            style =
                MaterialTheme.typography.titleMedium,
            color = onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = label,
            style =
                MaterialTheme.typography.labelSmall,
            color =
                onAccent.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AppIcon(
    letter: String,
    size: Dp,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(size * 0.32f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor =
                    colors.primary.copy(alpha = 0.5f),
                spotColor =
                    colors.primary.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(accentBrush()),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = letter,
            style =
                MaterialTheme.typography.titleLarge,
            color = colors.onPrimary
        )
    }
}

@Composable
private fun StatusPill(
    available: Boolean
) {

    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (available)
                    colors.primaryContainer
                else
                    colors.outlineVariant.copy(
                        alpha = 0.5f
                    )
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    if (available)
                        colors.primary
                    else
                        colors.onSurfaceVariant,
                    CircleShape
                )
        )

        Text(
            text =
                if (available)
                    "Tersedia"
                else
                    "Segera",
            style =
                MaterialTheme.typography.labelSmall,
            color =
                if (available)
                    colors.onPrimaryContainer
                else
                    colors.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturedCard(
    app: RexModule,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(32.dp)

    val primary = colors.primary
    val outline = colors.outlineVariant

    val transition =
        rememberInfiniteTransition(
            label = "featured"
        )

    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue =
            if (isActive) 0.9f else 0.25f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    2600,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressable(
                enabled = app.available,
                onClick = onClick
            )
            .alpha(
                if (app.available) 1f else 0.6f
            )
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor =
                    primary.copy(alpha = 0.3f),
                spotColor =
                    primary.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(colors.surfaceVariant)
            .drawBehind {

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(
                                alpha = 0.28f * glow
                            ),
                            Color.Transparent
                        ),
                        center = Offset(
                            size.width * 0.9f,
                            0f
                        ),
                        radius =
                            size.width * 0.75f
                    ),
                    radius =
                        size.width * 0.75f,
                    center = Offset(
                        size.width * 0.9f,
                        0f
                    )
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        primary.copy(
                            alpha =
                                0.3f +
                                    0.6f * glow
                        ),
                        outline
                    )
                ),
                shape = shape
            )
            .padding(24.dp)
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {

                AppIcon(
                    letter =
                        app.name
                            .take(1)
                            .uppercase(),
                    size = 64.dp
                )

                StatusPill(
                    available = app.available
                )
            }

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            colors.primary.copy(
                                alpha = 0.12f
                            )
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        text = "Aplikasi utama",
                        style =
                            MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }

                Text(
                    text = app.name,
                    style =
                        MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text = app.description,
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            if (app.available) {

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentBrush())
                        .padding(
                            horizontal = 20.dp,
                            vertical = 11.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "Buka",
                        style =
                            MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )

                    Icon(
                        imageVector =
                            Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier =
                            Modifier.size(16.dp)
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

    val shape =
        RoundedCornerShape(24.dp)

    val borderColor =
        if (app.available) {
            colors.primary.copy(alpha = 0.35f)
        } else {
            colors.outlineVariant
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(166.dp)
            .pressable(
                enabled = app.available,
                onClick = onClick
            )
            .alpha(
                if (app.available) 1f else 0.55f
            )
            .shadow(
                elevation =
                    if (app.available)
                        4.dp
                    else
                        0.dp,
                shape = shape,
                ambientColor =
                    colors.primary.copy(alpha = 0.2f),
                spotColor =
                    colors.primary.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(
                1.dp,
                borderColor,
                shape
            )
            .padding(18.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {

                AppIcon(
                    letter =
                        app.name
                            .take(1)
                            .uppercase(),
                    size = 48.dp
                )

                if (app.available) {

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                colors.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint =
                                colors.onPrimaryContainer,
                            modifier =
                                Modifier.size(14.dp)
                        )
                    }

                } else {

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                colors.outlineVariant.copy(
                                    alpha = 0.5f
                                )
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                    ) {

                        Text(
                            text = "Segera",
                            style =
                                MaterialTheme.typography.labelSmall,
                            color =
                                colors.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    text = app.name,
                    style =
                        MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text = app.description,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* --------------------------------- PROFILE -------------------------------- */

/**
 * Data satu tombol sosial media.
 */
private data class SocialLink(
    val title: String,
    val subtitle: String,
    val symbol: String,
    val color: Color,
    val url: String
)

private val socialLinks = listOf(

    SocialLink(
        title = "YouTube",
        subtitle = "@RexRuinCraft",
        symbol = "▶",
        color = Color(0xFFFF0000),
        url = "https://www.youtube.com/@RexRuinCraft"
    ),

    SocialLink(
        title = "TikTok",
        subtitle = "@rexstudio4",
        symbol = "♪",
        color = Color(0xFF111111),
        url = "https://www.tiktok.com/@rexstudio4"
    ),

    SocialLink(
        title = "Saweria",
        subtitle = "Dukung lewat donasi",
        symbol = "☕",
        color = Color(0xFFF5A623),
        url = "https://saweria.co/rexruincraft"
    ),

    SocialLink(
        title = "Saluran WhatsApp",
        subtitle = "Gabung untuk update terbaru",
        symbol = "✆",
        color = Color(0xFF25D366),
        url = "https://whatsapp.com/channel/0029Vb8oqCTDOQIYrRuWic33"
    )
)

@Composable
private fun ProfileContent(
    padding: PaddingValues,
    appCount: Int,
    themeLabel: String,
    isActive: Boolean
) {

    val colors = MaterialTheme.colorScheme

    val spin =
        rememberInfiniteTransition(
            label = "spin"
        )

    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue =
            if (isActive) 360f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    8000,
                    easing = LinearEasing
                )
            ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.primary.copy(
                            alpha = 0.14f
                        ),
                        colors.background,
                        colors.background
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(padding)
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            /* -------------------- AVATAR -------------------- */

            Box(
                modifier = Modifier
                    .entrance(0)
                    .size(132.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor =
                            colors.primary.copy(
                                alpha = 0.4f
                            ),
                        spotColor =
                            colors.primary.copy(
                                alpha = 0.4f
                            )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = angle
                        }
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    colors.primary,
                                    colors.tertiary,
                                    colors.primary
                                )
                            ),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .background(
                            colors.background,
                            CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(106.dp)
                            .background(
                                accentBrush(),
                                CircleShape
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "R",
                            style =
                                MaterialTheme.typography.headlineLarge,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                colors.onPrimary
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            /* -------------------- TITLE -------------------- */

            Text(
                text = "Profile RexAps",
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center,
                modifier =
                    Modifier.entrance(1)
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Kelola aplikasi dan preferensimu",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    colors.onSurfaceVariant,
                textAlign =
                    TextAlign.Center,
                modifier =
                    Modifier.entrance(1)
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            /* -------------------- STATISTIK -------------------- */

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(2),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                StatCard(
                    label = "Aplikasi",
                    value = "$appCount",
                    modifier =
                        Modifier.weight(1f)
                )

                StatCard(
                    label = "Tema",
                    value = themeLabel,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            /* -------------------- SECTION TITLE -------------------- */

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(3),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(
                            width = 4.dp,
                            height = 20.dp
                        )
                        .background(
                            accentBrush(),
                            RoundedCornerShape(50)
                        )
                )

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

                Column {

                    Text(
                        text = "Ikuti & Dukung Rex",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            "Temukan aku di platform favoritmu",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            colors.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            /* -------------------- SOCIAL LINKS -------------------- */

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                socialLinks.forEachIndexed { index, link ->

                    SocialLinkCard(
                        link = link,
                        modifier =
                            Modifier.entrance(4 + index)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            /* -------------------- FOOTER -------------------- */

            Text(
                text = "Dibuat dengan ❤ oleh Rex",
                style =
                    MaterialTheme.typography.labelSmall,
                color =
                    colors.onSurfaceVariant.copy(
                        alpha = 0.7f
                    ),
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
    }
}

@Composable
private fun SocialLinkCard(
    link: SocialLink,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme

    val uriHandler =
        LocalUriHandler.current

    val interaction =
        remember {
            MutableInteractionSource()
        }

    val pressed by
        interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue =
            if (pressed) 0.96f else 1f,
        animationSpec =
            spring(
                dampingRatio = 0.55f,
                stiffness = 400f
            ),
        label = "pressScale"
    )

    Surface(
        onClick = {

            try {
                uriHandler.openUri(link.url)
            } catch (_: Exception) {
                // Tidak ada aplikasi/browser
                // untuk membuka link.
            }
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(20.dp),
        color =
            colors.surfaceVariant.copy(
                alpha = 0.45f
            ),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        link.color.copy(
                            alpha = 0.55f
                        ),
                        link.color.copy(
                            alpha = 0.08f
                        )
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /* ---------------- ICON ---------------- */

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor =
                            link.color.copy(
                                alpha = 0.5f
                            ),
                        spotColor =
                            link.color.copy(
                                alpha = 0.5f
                            )
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(
                                link.color,
                                link.color.copy(
                                    alpha = 0.7f
                                )
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = link.symbol,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.size(14.dp)
            )

            /* ---------------- TEXT ---------------- */

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = link.title,
                    style =
                        MaterialTheme.typography.titleSmall,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = link.subtitle,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        colors.onSurfaceVariant
                )
            }

            /* ---------------- ARROW ---------------- */

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        link.color.copy(
                            alpha = 0.14f
                        ),
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "↗",
                    color =
                        link.color.copy(
                            alpha = 0.95f
                        ),
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

/* --------------------------------- STAT CARD ------------------------------ */

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(
                1.dp,
                colors.outlineVariant,
                shape
            )
            .padding(18.dp),
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = label,
            style =
                MaterialTheme.typography.labelMedium,
            color =
                colors.onSurfaceVariant
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography.titleLarge,
            color = colors.primary,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}

/* -------------------------------- SETTINGS -------------------------------- */

@Composable
private fun SettingsContent(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onThemeSelected:
        (RexThemeOption) -> Unit
) {

    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding =
            PaddingValues(
                horizontal = 20.dp,
                vertical = 24.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {

            Text(
                text = "Pengaturan",
                style =
                    MaterialTheme.typography.headlineLarge,
                modifier =
                    Modifier.entrance(0)
            )
        }

        item {

            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .entrance(1)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = "Tema",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Surface(
                        shape = CircleShape,
                        color =
                            colors.primaryContainer
                    ) {

                        Text(
                            text =
                                "${RexThemeOption.values().size} pilihan",
                            style =
                                MaterialTheme.typography.labelSmall,
                            color =
                                colors.onPrimaryContainer,
                            modifier =
                                Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                )
                        )
                    }
                }

                Text(
                    text =
                        "Aktif: ${selectedTheme.label}. Pilihanmu tersimpan otomatis.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        colors.onSurfaceVariant,
                    modifier =
                        Modifier.padding(top = 4.dp)
                )
            }
        }

        item {

            ThemePicker(
                selected = selectedTheme,
                onSelect = onThemeSelected,
                modifier =
                    Modifier.entrance(2)
            )
        }

        item {

            val shape =
                RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .entrance(3)
                    .clip(shape)
                    .background(
                        colors.surfaceVariant
                    )
                    .border(
                        1.dp,
                        colors.outlineVariant,
                        shape
                    )
                    .padding(16.dp),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                AppIcon(
                    letter = "R",
                    size = 48.dp
                )

                Column {

                    Text(
                        text = "RexAps",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            "Semua aplikasimu, satu tempat.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            colors.onSurfaceVariant
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
        containerColor =
            MaterialTheme.colorScheme.background,
        contentColor =
            MaterialTheme.colorScheme.onBackground
    ) {

        Column(
            modifier = Modifier
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 32.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Column {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = "Tema",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Surface(
                        shape = CircleShape,
                        color =
                            MaterialTheme.colorScheme.primaryContainer
                    ) {

                        Text(
                            text =
                                "${RexThemeOption.values().size}",
                            style =
                                MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier =
                                Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                )
                        )
                    }
                }

                Text(
                    text =
                        "Pilih tampilan yang paling nyaman untukmu.",
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ThemePicker(
                selected = selected,
                onSelect = onSelect
            )
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
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        RexThemeOption
            .values()
            .toList()
            .chunked(THEME_COLUMNS)
            .forEach { rowItems ->

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    rowItems.forEach { option ->

                        ThemeTile(
                            option = option,
                            isSelected =
                                option == selected,
                            onClick = {
                                onSelect(option)
                            },
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    repeat(
                        THEME_COLUMNS -
                            rowItems.size
                    ) {

                        Spacer(
                            modifier =
                                Modifier.weight(1f)
                        )
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

    val colors =
        MaterialTheme.colorScheme

    val spec =
        remember(option) {
            RexPaletteSpec.of(option)
        }

    val previewBg =
        Color(spec.background)

    val previewCard =
        Color(spec.card)

    val previewOutline =
        Color(spec.outline)

    val previewPrimary =
        Color(spec.primary)

    val previewTertiary =
        Color(spec.tertiary)

    val shape =
        RoundedCornerShape(18.dp)

    val previewShape =
        RoundedCornerShape(12.dp)

    val borderColor by animateColorAsState(
        targetValue =
            if (isSelected)
                colors.primary
            else
                colors.outlineVariant,
        animationSpec =
            tween(250),
        label = "tileBorder"
    )

    val tileScale by animateFloatAsState(
        targetValue =
            if (isSelected) 1f else 0.96f,
        animationSpec =
            spring(
                stiffness =
                    Spring.StiffnessMediumLow
            ),
        label = "tileScale"
    )

    val checkScale by animateFloatAsState(
        targetValue =
            if (isSelected) 1f else 0f,
        animationSpec =
            spring(
                dampingRatio =
                    Spring.DampingRatioMediumBouncy
            ),
        label = "check"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
            }
            .clip(shape)
            .background(
                colors.surfaceVariant
            )
            .border(
                if (isSelected) 2.dp else 1.dp,
                borderColor,
                shape
            )
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(6.dp),
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(previewShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            previewBg,
                            previewCard
                        )
                    )
                )
                .border(
                    1.dp,
                    previewOutline,
                    previewShape
                )
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(16.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                previewPrimary,
                                previewTertiary
                            )
                        ),
                        CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha =
                            checkScale.coerceIn(
                                0f,
                                1f
                            )
                    }
                    .background(
                        colors.primary,
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Check,
                    contentDescription =
                        "Dipilih",
                    tint =
                        colors.onPrimary,
                    modifier =
                        Modifier.size(12.dp)
                )
            }
        }

        Text(
            text = option.label,
            style =
                MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            textAlign =
                TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        )
    }
}