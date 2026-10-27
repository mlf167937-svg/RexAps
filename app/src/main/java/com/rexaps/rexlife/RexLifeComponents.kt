package com.rexaps.rexlife

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val RexLifeDays = listOf(
    "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
)

@Composable
internal fun RexLifePageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    var isPressed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300, easing = EaseOutCubic)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceVariant.copy(alpha = .78f))
                .border(1.dp, colors.outlineVariant.copy(alpha = .65f), RoundedCornerShape(16.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke()
    }
}

@Composable
internal fun RexLifeSectionTitle(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(200, easing = EaseOutCubic)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.invoke()
    }
}

@Composable
internal fun RexLifeFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .animateContentSize(animationSpec = tween(300, easing = EaseOutCubic))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .scale(if (isPressed) 0.98f else 1f),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = c.surface.copy(alpha = .96f)),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = .75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(104.dp)
                    .offset(x = 24.dp, y = (-24).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = .18f), Color.Transparent)
                        )
                    )
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.copy(alpha = .20f), c.tertiary.copy(alpha = .12f))
                                )
                            )
                            .border(1.dp, accent.copy(alpha = .24f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
                    }
                    if (active) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Buka $title", tint = c.onSurfaceVariant)
                    } else {
                        Surface(
                            color = c.surfaceVariant,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, c.outlineVariant)
                        ) {
                            Text(
                                "Segera",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = c.onSurfaceVariant
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun RexLifeHero(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val infiniteTransition = rememberInfiniteTransition(label = "HeroPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.primary, c.tertiary)))
            .border(1.dp, c.onPrimary.copy(alpha = .16f), shape)
            .padding(22.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-30).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(c.onPrimary.copy(alpha = glowAlpha))
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = c.onPrimary.copy(alpha = .14f),
                contentColor = c.onPrimary,
                shape = CircleShape
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp))
                    Text("REXAPS · LIFESTYLE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = c.onPrimary
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = c.onPrimary.copy(alpha = .86f)
            )
        }
    }
}

@Composable
internal fun RexLifeEmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.surfaceVariant.copy(alpha = .54f))
            .border(1.dp, c.outlineVariant.copy(alpha = .65f), RoundedCornerShape(22.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(c.primary.copy(alpha = .11f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = c.primary, modifier = Modifier.size(27.dp)) }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun RexLifeStorageCard(
    store: RexLifeStore,
    onRequestAccess: () -> Unit,
    onRetry: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceVariant.copy(alpha = .65f)),
        border = BorderStroke(1.dp, if (store.usingPublicFolder) c.primary.copy(alpha = .32f) else c.outlineVariant)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.FolderOpen, null, tint = c.primary, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Penyimpanan data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (store.usingPublicFolder) "Folder Download RexAps aktif" else "Fallback penyimpanan aplikasi",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (store.usingPublicFolder) c.primary else c.onSurfaceVariant
                    )
                }
                if (!store.usingPublicFolder) Icon(Icons.Default.Info, null, tint = c.onSurfaceVariant)
            }
            Text(
                store.activeStoragePath,
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant
            )
            if (!store.usingPublicFolder) {
                Text(
                    "Agar data masuk ke ${RexLifeStore.PUBLIC_FOLDER_PATH}, aktifkan izin akses semua file untuk RexAps. Jika belum, data tetap tersimpan di penyimpanan privat aplikasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRequestAccess, modifier = Modifier.weight(1f)) {
                        Text("Atur akses")
                    }
                    OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                        Text("Coba lagi")
                    }
                }
            } else {
                Text(
                    "Data disimpan lokal dalam file JSON dan tidak memerlukan koneksi internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant
                )
            }
            if (store.storageError) {
                Text(
                    "Akses folder berubah. Data terbaru dialihkan ke penyimpanan privat agar tidak hilang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.error
                )
            }
        }
    }
}

/**
 * Enhanced card dengan animated state changes dan better visual feedback.
 */
@Composable
internal fun RexLifeInteractiveCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.97f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        content()
    }
}

/**
 * Tab dengan animated underline.
 */
@Composable
internal fun AnimatedTabIndicator(
    selectedTabIndex: Int,
    tabCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedPosition by animateDpAsState(
        targetValue = (100.dp / tabCount) * selectedTabIndex,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "TabPosition"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .offset(x = animatedPosition, y = 0.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary)
    )
}
