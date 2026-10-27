package com.rexaps.rexlife

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class RexLifeFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val active: Boolean,
    val badgeCount: Int = 0
)

@Composable
fun RexLifeScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val store = remember(appContext) { RexLifeStore(appContext) }
    var destination by rememberSaveable { mutableStateOf("home") }

    BackHandler(enabled = destination != "home") {
        destination = "home"
    }

    when (destination) {
        "savings" -> MySavingsScreen(store = store, onBack = { destination = "home" })
        "school" -> MySchoolScreen(store = store, onBack = { destination = "home" })
        "health" -> RexLifePlaceholderScreenEnhanced(
            title = "MyHealth",
            subtitle = "Bangun kebiasaan sehat secara bertahap.",
            detail = "Pencatatan kebiasaan, tidur, dan aktivitas pribadi akan dibuat pada versi berikutnya.",
            icon = Icons.Default.Favorite,
            onBack = { destination = "home" }
        )
        "tasks" -> RexLifePlaceholderScreenEnhanced(
            title = "MyTasks",
            subtitle = "Satu tempat untuk pekerjaan dan checklist.",
            detail = "MyTasks akan dikembangkan setelah fondasi MySavings dan MySchool stabil.",
            icon = Icons.Default.Checklist,
            onBack = { destination = "home" }
        )
        "notes" -> RexLifePlaceholderScreenEnhanced(
            title = "MyNotes",
            subtitle = "Simpan ide dan catatan penting.",
            detail = "Penyimpanan catatan dan lampiran belum diaktifkan di V1.",
            icon = Icons.Default.Notes,
            onBack = { destination = "home" }
        )
        "planner" -> RexLifePlaceholderScreenEnhanced(
            title = "MyPlanner",
            subtitle = "Rencanakan hari dengan lebih teratur.",
            detail = "Kalender terpadu akan ditambahkan setelah format data lintas fitur ditetapkan.",
            icon = Icons.Default.CalendarMonth,
            onBack = { destination = "home" }
        )
        else -> RexLifeDashboardEnhanced(
            store = store,
            onExit = onExit,
            onOpenFeature = { destination = it },
            onRequestAccess = { openStorageSettings(context) },
            onRetryStorage = { store.refreshStorage() }
        )
    }
}

@Composable
private fun RexLifeDashboardEnhanced(
    store: RexLifeStore,
    onExit: () -> Unit,
    onOpenFeature: (String) -> Unit,
    onRequestAccess: () -> Unit,
    onRetryStorage: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val features = listOf(
        RexLifeFeature("savings", "MySavings", "Saldo, transaksi, dan target tabungan.", Icons.Default.AccountBalanceWallet, true),
        RexLifeFeature("school", "MySchool", "Jadwal pelajaran, seragam, dan PR.", Icons.Default.School, true),
        RexLifeFeature("health", "MyHealth", "Kebiasaan dan kesehatan pribadi.", Icons.Default.Favorite, false),
        RexLifeFeature("tasks", "MyTasks", "To-do list dan pekerjaan harian.", Icons.Default.Checklist, false),
        RexLifeFeature("notes", "MyNotes", "Catatan, ide, dan informasi penting.", Icons.Default.Notes, false),
        RexLifeFeature("planner", "MyPlanner", "Kalender dan agenda pribadi.", Icons.Default.CalendarMonth, false)
    )
    val balance = store.data.transactions.sumOf {
        if (it.kind == "expense") -it.amount else it.amount
    }
    val pendingHomework = store.data.homework.count { !it.done }
    val analytics = store.data.getAnalytics()
    
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedGap(20.dp)
    ) {
        RexLifePageHeader(
            title = "RexLife",
            subtitle = "Ruang pribadi di dalam RexAps",
            onBack = onExit
        )
        
        // Enhanced Hero dengan animated glow
        RexLifeHero(
            title = "Kelola hidupmu.",
            description = "Tabungan, sekolah, dan rencana harian dalam satu ruang yang rapi."
        )
        
        // Quick stats dengan smooth animations
        Row(
            horizontalArrangement = Arrangement.spacedGap(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnhancedDashboardMetric(
                label = "Saldo bersih",
                value = formatRupiah(balance),
                icon = Icons.Default.AccountBalanceWallet,
                color = c.primary,
                modifier = Modifier.weight(1f)
            )
            EnhancedDashboardMetric(
                label = "PR belum selesai",
                value = pendingHomework.toString(),
                icon = Icons.Default.School,
                color = c.tertiary,
                modifier = Modifier.weight(1f),
                isAlert = pendingHomework > 0
            )
        }
        
        // Analytics Snapshot
        if (analytics.topCategories.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = c.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, c.outlineVariant.copy(alpha = 0.7f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedGap(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Analisis Cepat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Lanjut →",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.primary
                        )
                    }
                    
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedGap(8.dp)
                    ) {
                        StatBox(
                            label = "Pemasukan",
                            value = formatRupiah(analytics.totalIncome),
                            modifier = Modifier.weight(1f),
                            color = c.primary
                        )
                        StatBox(
                            label = "Pengeluaran",
                            value = formatRupiah(analytics.totalExpense),
                            modifier = Modifier.weight(1f),
                            color = c.error
                        )
                    }
                    
                    SmoothProgressIndicator(
                        progress = ((analytics.savingsRate + 100) / 200).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (analytics.savingsRate > 0) c.primary else c.error
                    )
                    Text(
                        "Laju tabungan: ${String.format("%.1f", analytics.savingsRate)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onSurfaceVariant
                    )
                }
            }
        }
        
        // Features Grid dengan stagger animation
        Column(verticalArrangement = Arrangement.spacedGap(4.dp)) {
            RexLifeSectionTitle(
                title = "Ruang kerja",
                subtitle = "Pilih fitur untuk mulai mengelola data"
            )
            features.chunked(2).forEachIndexed { rowIndex, pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedGap(12.dp)
                ) {
                    pair.forEachIndexed { colIndex, feature ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                animationSpec = tween(500, delayMillis = (rowIndex * 2 + colIndex) * 80, easing = EaseOutCubic),
                                initialOffsetY = { it }
                            ) + fadeIn(animationSpec = tween(500, delayMillis = (rowIndex * 2 + colIndex) * 80)),
                            modifier = Modifier.weight(1f)
                        ) {
                            RexLifeFeatureCardEnhanced(
                                title = feature.title,
                                description = feature.description,
                                icon = feature.icon,
                                active = feature.active,
                                badgeCount = feature.badgeCount,
                                onClick = { onOpenFeature(feature.id) },
                                accent = if (feature.id == "school") c.tertiary else c.primary
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        
        RexLifeStorageCard(
            store = store,
            onRequestAccess = onRequestAccess,
            onRetry = onRetryStorage
        )
        
        // Footer info
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedGap(8.dp)
        ) {
            Text(
                "RexLife V1 · Lokal, offline, tanpa notifikasi",
                style = MaterialTheme.typography.labelSmall,
                color = c.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedGap(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = c.primary, modifier = Modifier.size(14.dp))
                Text(
                    "Dibuat dengan perhatian detail",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RexLifeFeatureCardEnhanced(
    title: String,
    description: String,
    icon: ImageVector,
    active: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .animateContentSize(animationSpec = tween(300, easing = EaseOutCubic))
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = c.surface.copy(alpha = .96f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.outlineVariant.copy(alpha = .75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Background glow
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(104.dp)
                    .offset(x = 24.dp, y = (-24).dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = .18f), androidx.compose.ui.graphics.Color.Transparent)
                        )
                    )
            )
            
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedGap(10.dp)
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
                    
                    if (badgeCount > 0) {
                        PulseAnimation(isActive = true) {
                            Surface(
                                color = c.error,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(28.dp),
                                contentColor = c.onError
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else if (active) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Buka $title", tint = c.onSurfaceVariant)
                    } else {
                        Surface(
                            color = c.surfaceVariant,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, c.outlineVariant)
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
                
                Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedGap(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedDashboardMetric(
    label: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    val c = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.animateContentSize(animationSpec = tween(300, easing = EaseOutCubic)),
        color = c.surfaceVariant.copy(alpha = .72f),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAlert) c.error.copy(alpha = 0.5f) else c.outlineVariant.copy(alpha = .7f))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedGap(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedGap(6.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                if (isAlert) {
                    PulseAnimation(isActive = true) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = c.error, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedGap(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
            AnimatedNumberCounter(value.replace("Rp", "").replace(".", "").toLongOrNull() ?: 0L)
        }
    }
}

@Composable
private fun RexLifePlaceholderScreenEnhanced(
    title: String,
    subtitle: String,
    detail: String,
    icon: ImageVector,
    onBack: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedGap(20.dp)
    ) {
        RexLifePageHeader(title = title, subtitle = subtitle, onBack = onBack)
        
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = tween(500, easing = EaseOutCubic)) + fadeIn(animationSpec = tween(500))
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                c.primary.copy(alpha = .13f),
                                c.tertiary.copy(alpha = .10f)
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedGap(14.dp)
                ) {
                    Icon(icon, null, tint = c.primary, modifier = Modifier.size(56.dp))
                    Text("Segera Hadir", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.titleMedium)
                    Text(detail, color = c.onSurfaceVariant)
                    OutlinedButton(onClick = onBack) { Text("Kembali ke RexLife") }
                }
            }
        }
    }
}

private fun openStorageSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
}

internal fun formatRupiah(amount: Long): String {
    val sign = if (amount < 0) "- " else ""
    val digits = kotlin.math.abs(amount).toString().reversed().chunked(3).joinToString(".").reversed()
    return "${sign}Rp$digits"
}
