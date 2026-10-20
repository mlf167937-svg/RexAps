// RexApsApp.kt
package com.rexaps.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.rexaps.rexgit.RexGit
import com.rexaps.rexmanager.RexManagerScreen
import com.rexaps.rexmusic.RexMusicScreen
import com.rexaps.rexnux.RexNuxScreen
import com.rexaps.rexspacex.RexSpaceXScreen
import com.rexaps.rextools.navigation.RexNavGraph
import kotlinx.coroutines.delay

/* ═════════════════════ Model & shell ═════════════════════ */

internal data class BottomTab(val label: String, val icon: ImageVector)

internal val bottomTabs = listOf(
    BottomTab("Home", Icons.Default.Home),
    BottomTab("Profile", Icons.Default.Person),
    BottomTab("Theme", Icons.Default.ColorLens),
    BottomTab("Settings", Icons.Default.Settings),
    BottomTab("Account", Icons.Default.AccountCircle)
)

internal object RexTab {
    const val HOME = 0
    const val PROFILE = 1
    const val THEME = 2
    const val SETTINGS = 3
    const val ACCOUNT = 4
}

internal const val THEME_COLUMNS = 3

/** State & aksi yang dibagikan ke shell portrait maupun landscape. */
internal class RexShell(
    val selectedTab: Int,
    val themeOption: RexThemeOption,
    val onSelectTab: (Int) -> Unit,
    val onAppClick: (RexModule) -> Unit,
    val onOpenTheme: () -> Unit
)

/* ═════════════════════ ROOT ═════════════════════ */

@Composable
fun RexApsApp(activity: Activity) {

    var route by remember { mutableStateOf(RexRoute.NONE) }
    var routeLoading by remember { mutableStateOf(false) }

    val themeStore = remember { RexThemeStore(activity) }
    var themeOption by remember { mutableStateOf(themeStore.load()) }

    var themeApplying by remember { mutableStateOf(false) }
    var pendingTheme by remember { mutableStateOf<RexThemeOption?>(null) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(RexTab.HOME) }

    val onThemeChange: (RexThemeOption) -> Unit = { option ->
        if (option != themeOption) {
            pendingTheme = option
            themeApplying = true
        }
    }

    /* SUB-APP ROUTES */
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
                        onFinished = { routeLoading = false }
                    )
                } else {
                    when (route) {
                        RexRoute.REXFOX -> RexFoxScreen(activity = activity)

                        RexRoute.REXPANEL ->
                            com.rexaps.rexpanel.RexPanelScreen(
                                activity = activity,
                                onExit = { route = RexRoute.NONE }
                            )

                        RexRoute.REXCHAT ->
                            com.rexaps.rexchat.SessionListScreen(
                                onOpen = { session, method ->
                                    com.rexaps.rexchat.openRexChatSession(activity, session, method)
                                },
                                onExit = { route = RexRoute.NONE }
                            )

                        RexRoute.REXNUX ->
                            RexNuxScreen(onExit = { route = RexRoute.NONE })

                        RexRoute.REXTOOLS ->
                            RexNavGraph(onBack = { route = RexRoute.NONE })

                        RexRoute.REXMUSIC ->
                            RexMusicScreen(onBack = { route = RexRoute.NONE })

                        RexRoute.REXMANAGER ->
                            RexManagerScreen(onExit = { route = RexRoute.NONE })

                        RexRoute.REXGIT -> RexGit()

                        RexRoute.REXCODER ->
                            RexCoderScreen(
                                activity = activity,
                                onExit = { route = RexRoute.NONE }
                            )

                        RexRoute.REXMONITOR ->
                            com.rexaps.rexmonitor.RexMonitorScreen(
                                onExit = { route = RexRoute.NONE }
                            )

                        RexRoute.REXSPACEX ->
                            RexSpaceXScreen(onExit = { route = RexRoute.NONE })

                        RexRoute.REXWARP ->
                            com.rexaps.rexwarp.RexWarpScreen(
                                onExit = { route = RexRoute.NONE }
                            )

                        RexRoute.NONE -> Unit
                    }
                }
            }
        }
        return
    }

    /* MAIN SHELL — portrait / landscape */
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val shell = RexShell(
        selectedTab = selectedTab,
        themeOption = themeOption,
        onSelectTab = { selectedTab = it },
        onAppClick = { app ->
            if (app.available) {
                RexRoute.fromModuleId(app.id)?.let {
                    route = it
                    routeLoading = true
                }
            }
        },
        onOpenTheme = { showThemeSheet = true }
    )

    RexTheme(option = themeOption) {

        if (isLandscape) {
            RexAppsScreenLandscape(shell)
        } else {
            RexAppsScreenPortrait(shell)
        }

        if (showThemeSheet) {
            ThemeSheet(
                selected = themeOption,
                onSelect = onThemeChange,
                onDismiss = { showThemeSheet = false }
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

/* ═════════════════════ Animasi & util ═════════════════════ */

@Composable
internal fun Modifier.entrance(index: Int): Modifier {

    var shown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!shown) delay(index.coerceAtMost(8) * 60L)
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
internal fun accentBrush(): Brush =
    Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

/* ═════════════════════ Komponen bersama app ═════════════════════ */

@Composable
internal fun AppIcon(letter: String, size: Dp, modifier: Modifier = Modifier) {

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
internal fun SectionHeader(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 20.dp)
                .background(accentBrush(), RoundedCornerShape(50))
        )
        Spacer(Modifier.size(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun StatusPill(available: Boolean) {

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
internal fun HomeHeader(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colors.primary, CircleShape)
                )
                Text(
                    text = "Selamat datang di",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
            Text(
                text = "RexAps",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = colors.primary.copy(alpha = 0.4f),
                    spotColor = colors.primary.copy(alpha = 0.4f)
                )
                .pressable(onClick = onOpenSettings)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outlineVariant, CircleShape)
                .semantics { contentDescription = "Buka pengaturan" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Pengaturan",
                tint = colors.primary,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
internal fun HeroCard(
    appCount: Int,
    availableCount: Int,
    themeLabel: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary

    val transition = rememberInfiniteTransition(label = "hero")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(accentBrush())
            .drawBehind {
                drawCircle(
                    color = onAccent.copy(alpha = 0.14f),
                    radius = 120.dp.toPx(),
                    center = Offset(
                        x = size.width - 30.dp.toPx() - drift * 30.dp.toPx(),
                        y = 10.dp.toPx() + drift * 24.dp.toPx()
                    )
                )
                drawCircle(
                    color = onAccent.copy(alpha = 0.10f),
                    radius = 70.dp.toPx(),
                    center = Offset(
                        x = size.width - 90.dp.toPx() + drift * 36.dp.toPx(),
                        y = size.height - 10.dp.toPx() - drift * 20.dp.toPx()
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(onAccent.copy(alpha = 0.18f))
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStatChip(label = "Tersedia", value = "$availableCount", onAccent = onAccent)
                HeroStatChip(label = "Tema aktif", value = themeLabel, onAccent = onAccent)
            }
        }
    }
}

@Composable
private fun HeroStatChip(label: String, value: String, onAccent: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(onAccent.copy(alpha = 0.12f))
            .border(1.dp, onAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onAccent.copy(alpha = 0.8f)
        )
    }
}

@Composable
internal fun FeaturedCard(
    app: RexModule,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val primary = colors.primary
    val outline = colors.outlineVariant

    val transition = rememberInfiniteTransition(label = "featured")
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isActive) 0.9f else 0.25f,
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
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.3f),
                spotColor = primary.copy(alpha = 0.3f)
            )
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
            .padding(24.dp)
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

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Aplikasi utama",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
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
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppCard(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val borderColor =
        if (app.available) colors.primary.copy(alpha = 0.35f) else colors.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(166.dp)
            .pressable(enabled = app.available, onClick = onClick)
            .alpha(if (app.available) 1f else 0.55f)
            .shadow(
                elevation = if (app.available) 4.dp else 0.dp,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = 0.2f),
                spotColor = colors.primary.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, borderColor, shape)
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
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
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

@Composable
internal fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {

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

/* ═════════════════════ Profil app: sosial & avatar ═════════════════════ */

internal data class SocialLink(
    val title: String,
    val subtitle: String,
    val symbol: String,
    val color: Color,
    val url: String
)

internal val socialLinks = listOf(
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
internal fun SocialLinkCard(link: SocialLink, modifier: Modifier = Modifier) {

    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "pressScale"
    )

    Surface(
        onClick = {
            try {
                uriHandler.openUri(link.url)
            } catch (_: Exception) {
                // Tidak ada aplikasi/browser untuk membuka link.
            }
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(link.color.copy(alpha = 0.55f), link.color.copy(alpha = 0.08f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = link.color.copy(alpha = 0.5f),
                        spotColor = link.color.copy(alpha = 0.5f)
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(link.color, link.color.copy(alpha = 0.7f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = link.symbol,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.size(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = link.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(link.color.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↗",
                    color = link.color.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Avatar berputar dengan ring warna tema. */
@Composable
internal fun SpinningAvatar(isActive: Boolean, size: Dp = 132.dp) {

    val colors = MaterialTheme.colorScheme
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 20.dp,
                shape = CircleShape,
                ambientColor = colors.primary.copy(alpha = 0.4f),
                spotColor = colors.primary.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = angle }
                .background(
                    Brush.sweepGradient(
                        listOf(colors.primary, colors.tertiary, colors.primary)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .background(colors.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .background(accentBrush(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }
    }
}

/* ═════════════════════ Tema ═════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSheet(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pilih tema",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${RexThemeOption.values().size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "30 tampilan premium. Pilih yang paling nyaman untukmu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ThemePicker(selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
internal fun ThemePicker(
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
                    Spacer(Modifier.weight(1f))
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
    val spec = remember(option) { RexPaletteSpec.of(option) }

    val previewBg = Color(spec.background)
    val previewCard = Color(spec.card)
    val previewOutline = Color(spec.outline)
    val previewPrimary = Color(spec.primary)
    val previewTertiary = Color(spec.tertiary)

    val shape = RoundedCornerShape(18.dp)
    val previewShape = RoundedCornerShape(12.dp)

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
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(previewShape)
                .background(Brush.linearGradient(listOf(previewBg, previewCard)))
                .border(1.dp, previewOutline, previewShape)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(16.dp)
                    .background(
                        Brush.linearGradient(listOf(previewPrimary, previewTertiary)),
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
                        alpha = checkScale.coerceIn(0f, 1f)
                    }
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Dipilih",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        )
    }
}

@Composable
internal fun ThemeTabContent(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onOpenTheme: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tema", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personalisasikan tampilan RexAps dengan 30 pilihan tema.", color = colors.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTheme),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ColorLens, null, tint = colors.onPrimary, modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Tema aktif", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(selectedTheme.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ketuk untuk memilih tema", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}