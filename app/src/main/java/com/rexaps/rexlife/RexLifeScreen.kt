package com.rexaps.rexlife

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class RexLifeFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val active: Boolean
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
        "health" -> RexLifePlaceholderScreen(
            title = "MyHealth",
            subtitle = "Bangun kebiasaan sehat secara bertahap.",
            detail = "Pencatatan kebiasaan, tidur, dan aktivitas pribadi akan dibuat pada versi berikutnya.",
            icon = Icons.Default.Favorite,
            onBack = { destination = "home" }
        )
        "tasks" -> RexLifePlaceholderScreen(
            title = "MyTasks",
            subtitle = "Satu tempat untuk pekerjaan dan checklist.",
            detail = "MyTasks akan dikembangkan setelah fondasi MySavings dan MySchool stabil.",
            icon = Icons.Default.Checklist,
            onBack = { destination = "home" }
        )
        "notes" -> RexLifePlaceholderScreen(
            title = "MyNotes",
            subtitle = "Simpan ide dan catatan penting.",
            detail = "Penyimpanan catatan dan lampiran belum diaktifkan di V1.",
            icon = Icons.Default.Notes,
            onBack = { destination = "home" }
        )
        "planner" -> RexLifePlaceholderScreen(
            title = "MyPlanner",
            subtitle = "Rencanakan hari dengan lebih teratur.",
            detail = "Kalender terpadu akan ditambahkan setelah format data lintas fitur ditetapkan.",
            icon = Icons.Default.CalendarMonth,
            onBack = { destination = "home" }
        )
        else -> RexLifeDashboard(
            store = store,
            onExit = onExit,
            onOpenFeature = { destination = it },
            onRequestAccess = { openStorageSettings(context) },
            onRetryStorage = { store.refreshStorage() }
        )
    }
}

@Composable
private fun RexLifeDashboard(
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
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        RexLifePageHeader(
            title = "RexLife",
            subtitle = "Ruang pribadi di dalam RexAps",
            onBack = onExit
        )
        RexLifeHero(
            title = "Kelola hidupmu.",
            description = "Tabungan, sekolah, dan rencana harian dalam satu ruang yang rapi."
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardMetric(
                label = "Saldo bersih",
                value = formatRupiah(balance),
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
            DashboardMetric(
                label = "PR belum selesai",
                value = store.data.homework.count { !it.done }.toString(),
                icon = Icons.Default.School,
                modifier = Modifier.weight(1f)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RexLifeSectionTitle(
                title = "Ruang kerja",
                subtitle = "Pilih fitur untuk mulai mengelola data"
            )
            features.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { feature ->
                        RexLifeFeatureCard(
                            title = feature.title,
                            description = feature.description,
                            icon = feature.icon,
                            active = feature.active,
                            onClick = { onOpenFeature(feature.id) },
                            modifier = Modifier.weight(1f),
                            accent = if (feature.id == "school") c.tertiary else c.primary
                        )
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
        Text(
            "RexLife V1 · Lokal, offline, tanpa notifikasi",
            style = MaterialTheme.typography.labelSmall,
            color = c.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = c.surfaceVariant.copy(alpha = .72f),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.outlineVariant.copy(alpha = .7f))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = c.primary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RexLifePlaceholderScreen(
    title: String,
    subtitle: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        RexLifePageHeader(title = title, subtitle = subtitle, onBack = onBack)
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(c.primary.copy(alpha = .13f), c.tertiary.copy(alpha = .10f))),
                RoundedCornerShape(28.dp)
            ).padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(icon, null, tint = c.primary, modifier = Modifier.size(56.dp))
                Text("Segera Hadir", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.titleMedium)
                Text(detail, color = c.onSurfaceVariant)
                OutlinedButton(onClick = onBack) { Text("Kembali ke RexLife") }
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
