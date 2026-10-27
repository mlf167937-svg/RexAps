package com.rexaps.rexlife

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
internal fun MySchoolScreen(
    store: RexLifeStore,
    onBack: () -> Unit
) {
    var section by rememberSaveable { mutableStateOf("overview") }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showUniformDialog by remember { mutableStateOf(false) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SchoolSubject?>(null) }
    var editingUniform by remember { mutableStateOf<UniformPlan?>(null) }
    var editingHomework by remember { mutableStateOf<Homework?>(null) }
    var deletingSubject by remember { mutableStateOf<SchoolSubject?>(null) }
    var deletingHomework by remember { mutableStateOf<Homework?>(null) }

    val data = store.data
    BackHandler(enabled = section != "overview" || showSubjectDialog || showUniformDialog || showHomeworkDialog ||
        deletingSubject != null || deletingHomework != null) {
        when {
            showSubjectDialog -> showSubjectDialog = false
            showUniformDialog -> showUniformDialog = false
            showHomeworkDialog -> showHomeworkDialog = false
            deletingSubject != null -> deletingSubject = null
            deletingHomework != null -> deletingHomework = null
            section != "overview" -> section = "overview"
        }
    }

    val pageTitle = when (section) {
        "schedule" -> "Jadwal pelajaran"
        "uniform" -> "Jadwal seragam"
        "homework" -> "Daftar PR"
        else -> "MySchool"
    }
    val pageSubtitle = when (section) {
        "schedule" -> "Atur mata pelajaran setiap minggu."
        "uniform" -> "Siapkan seragam sesuai hari."
        "homework" -> "Kelola tugas dan tanggal pengumpulan."
        else -> "Semua urusan sekolah dalam satu tempat."
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        RexLifePageHeader(
            title = pageTitle,
            subtitle = pageSubtitle,
            onBack = {
                if (section != "overview") section = "overview" else onBack()
            },
            trailing = if (section == "schedule" || section == "uniform" || section == "homework") {
                {
                    IconButton(onClick = {
                        when (section) {
                            "schedule" -> { editingSubject = null; showSubjectDialog = true }
                            "uniform" -> { editingUniform = null; showUniformDialog = true }
                            "homework" -> { editingHomework = null; showHomeworkDialog = true }
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "Tambah") }
                }
            } else null
        )

        when (section) {
            "schedule" -> ScheduleSectionEnhanced(
                subjects = data.subjects,
                onAdd = { editingSubject = null; showSubjectDialog = true },
                onEdit = { editingSubject = it; showSubjectDialog = true },
                onDelete = { deletingSubject = it }
            )
            "uniform" -> UniformSectionEnhanced(
                uniforms = data.uniforms,
                onEditDay = { day ->
                    editingUniform = data.uniforms.firstOrNull { it.day == day }
                    if (editingUniform == null) editingUniform = UniformPlan(day = day, outfit = "")
                    showUniformDialog = true
                }
            )
            "homework" -> HomeworkSectionEnhanced(
                homework = data.homework,
                onAdd = { editingHomework = null; showHomeworkDialog = true },
                onEdit = { editingHomework = it; showHomeworkDialog = true },
                onDelete = { deletingHomework = it },
                onToggleDone = { item ->
                    store.update { old ->
                        old.copy(homework = old.homework.map { if (it.id == item.id) it.copy(done = !it.done) else it })
                    }
                }
            )
            else -> MySchoolOverviewEnhanced(
                subjects = data.subjects,
                uniforms = data.uniforms,
                homework = data.homework,
                onSchedule = { section = "schedule" },
                onUniform = { section = "uniform" },
                onHomework = { section = "homework" },
                onAddHomework = { editingHomework = null; showHomeworkDialog = true }
            )
        }
        Spacer(Modifier.height(10.dp))
    }

    if (showSubjectDialog) {
        SubjectEditorDialog(
            initial = editingSubject,
            onDismiss = { showSubjectDialog = false },
            onSave = { saved ->
                store.update { old ->
                    old.copy(subjects = (old.subjects.filterNot { it.id == saved.id } + saved)
                        .sortedWith(compareBy<SchoolSubject> { RexLifeDays.indexOf(it.day).let { index -> if (index < 0) 99 else index } }.thenBy { it.startTime }))
                }
                showSubjectDialog = false
            }
        )
    }

    if (showUniformDialog) {
        UniformEditorDialog(
            initial = editingUniform,
            onDismiss = { showUniformDialog = false },
            onSave = { saved ->
                store.update { old ->
                    old.copy(uniforms = old.uniforms.filterNot { it.day == saved.day || it.id == saved.id } + saved)
                }
                showUniformDialog = false
            }
        )
    }

    if (showHomeworkDialog) {
        HomeworkEditorDialog(
            initial = editingHomework,
            scheduledSubjects = data.subjects.map { it.name }.distinct().sorted(),
            onDismiss = { showHomeworkDialog = false },
            onSave = { saved ->
                store.update { old ->
                    old.copy(homework = (old.homework.filterNot { it.id == saved.id } + saved)
                        .sortedWith(compareBy<Homework> { it.deadlineDate }.thenBy { it.deadlineTime }))
                }
                showHomeworkDialog = false
            }
        )
    }

    deletingSubject?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSubject = null },
            title = { Text("Hapus jadwal?") },
            text = { Text("${target.name} pada hari ${target.day} akan dihapus. PR yang sudah dibuat tidak ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = {
                    store.update { old -> old.copy(subjects = old.subjects.filterNot { it.id == target.id }) }
                    deletingSubject = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingSubject = null }) { Text("Batal") } }
        )
    }
    deletingHomework?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingHomework = null },
            title = { Text("Hapus PR?") },
            text = { Text("PR \"${target.description}\" akan dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    store.update { old -> old.copy(homework = old.homework.filterNot { it.id == target.id }) }
                    deletingHomework = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingHomework = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun MySchoolOverviewEnhanced(
    subjects: List<SchoolSubject>,
    uniforms: List<UniformPlan>,
    homework: List<Homework>,
    onSchedule: () -> Unit,
    onUniform: () -> Unit,
    onHomework: () -> Unit,
    onAddHomework: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val pending = homework.count { !it.done }
    val urgent = homework.count { !it.done && it.priority == 2 }

    RexLifeHero(
        title = "Sekolah lebih teratur.",
        description = "Jadwal, seragam, dan pekerjaan rumah bisa kamu atur sesuai kebutuhan."
    )
    
    // Enhanced metrics dengan animations
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SchoolMetricEnhanced(
            "Mata pelajaran",
            subjects.map { it.name }.distinct().size.toString(),
            Icons.Default.MenuBook,
            Modifier.weight(1f)
        )
        SchoolMetricEnhanced(
            "PR belum selesai",
            pending.toString(),
            Icons.Default.Event,
            Modifier.weight(1f),
            isAlert = urgent > 0
        )
    }
    
    RexLifeSectionTitle("Ruang kerja", "Pilih yang ingin kamu kelola.")
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        SchoolNavigationCardEnhanced(
            title = "Jadwal pelajaran",
            detail = if (subjects.isEmpty()) "Belum ada jadwal. Tambahkan jadwal mingguan." else "${subjects.size} sesi pelajaran terjadwal",
            icon = Icons.Default.CalendarMonth,
            onClick = onSchedule
        )
        SchoolNavigationCardEnhanced(
            title = "Jadwal seragam",
            detail = "${uniforms.size} dari ${RexLifeDays.size} hari sudah diatur",
            icon = Icons.Default.Checkroom,
            onClick = onUniform
        )
        SchoolNavigationCardEnhanced(
            title = "PR & tugas",
            detail = if (pending == 0) "Belum ada PR yang tertunda" else "$pending tugas menunggu diselesaikan",
            icon = Icons.Default.Book,
            onClick = onHomework,
            hasAlert = urgent > 0
        )
        FilledTonalButton(
            onClick = onAddHomework,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tambah PR Hari Ini")
        }
    }
    
    // Countdown to nearest deadline
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        RexLifeSectionTitle("Deadline terdekat", "Tugas yang belum selesai.")
        val upcoming = homework.filterNot { it.done }.sortedWith(compareBy<Homework> { it.deadlineDate }.thenBy { it.deadlineTime }).take(3)
        if (upcoming.isEmpty()) {
            RexLifeEmptyState(
                title = "Belum ada PR tertunda",
                description = "PR yang kamu tambahkan akan muncul di sini.",
                icon = Icons.Default.CheckCircle,
                actionLabel = "Tambah PR",
                onAction = onAddHomework
            )
        } else {
            upcoming.forEachIndexed { index, hw ->
                AnimatedVisibility(
                    visible = true,
                    enter = getCardEntranceAnimation(index),
                    exit = getCardExitAnimation(index)
                ) {
                    HomeworkCompactCardEnhanced(hw)
                }
            }
        }
    }
    Text(
        "V1 tidak membuat notifikasi atau alarm. Semua data disimpan lokal.",
        style = MaterialTheme.typography.bodySmall,
        color = c.onSurfaceVariant
    )
}

@Composable
private fun SchoolMetricEnhanced(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    isAlert: Boolean = false
) {
    val c = MaterialTheme.colorScheme
    val alertColor = if (isAlert) c.error else c.primary

    Surface(
        modifier = modifier.animateContentSize(animationSpec = tween(300, easing = EaseOutCubic)),
        shape = RoundedCornerShape(20.dp),
        color = c.surfaceVariant.copy(alpha = .65f),
        border = BorderStroke(1.dp, if (isAlert) c.error.copy(alpha = 0.5f) else c.outlineVariant.copy(alpha = .7f))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(alertColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = alertColor, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnimatedNumberCounter(value.toLongOrNull() ?: 0L)
                    if (isAlert) {
                        PulseAnimation(isActive = true) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchoolNavigationCardEnhanced(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    hasAlert: Boolean = false
) {
    val c = MaterialTheme.colorScheme

    RexLifeInteractiveCard(onClick = onClick) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(c.primary.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = c.primary, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (hasAlert) PulseAnimation(isActive = true) {
                            Surface(color = c.error.copy(alpha = 0.15f), shape = CircleShape) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp).padding(2.dp))
                            }
                        }
                    }
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ScheduleSectionEnhanced(
    subjects: List<SchoolSubject>,
    onAdd: () -> Unit,
    onEdit: (SchoolSubject) -> Unit,
    onDelete: (SchoolSubject) -> Unit
) {
    RexLifeSectionTitle("Jadwal mingguan", "Ketuk sesi untuk mengedit.")
    if (subjects.isEmpty()) {
        RexLifeEmptyState(
            title = "Jadwal masih kosong",
            description = "Tambahkan mata pelajaran, hari, dan jamnya. Jadwal ini nanti menjadi sumber pilihan mata pelajaran pada form PR.",
            icon = Icons.Default.CalendarMonth,
            actionLabel = "Buat jadwal",
            onAction = onAdd
        )
    } else {
        RexLifeDays.forEachIndexed { dayIndex, day ->
            val daySubjects = subjects.filter { it.day == day }.sortedBy { it.startTime }
            if (daySubjects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            animationSpec = tween(400, delayMillis = dayIndex * 50, easing = EaseOutCubic),
                            initialOffsetY = { it }
                        ) + fadeIn(animationSpec = tween(400, delayMillis = dayIndex * 50))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${daySubjects.size} pelajaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    daySubjects.forEachIndexed { subIndex, subject ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally(
                                animationSpec = tween(400, delayMillis = dayIndex * 50 + subIndex * 30, easing = EaseOutCubic),
                                initialOffsetX = { -it }
                            ) + fadeIn(animationSpec = tween(400, delayMillis = dayIndex * 50 + subIndex * 30))
                        ) {
                            SubjectCardEnhanced(subject, onClick = { onEdit(subject) }, onDelete = { onDelete(subject) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCardEnhanced(subject: SchoolSubject, onClick: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.colorScheme

    RexLifeInteractiveCard(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 13.dp, bottom = 13.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(subject.startTime, style = MaterialTheme.typography.labelMedium, color = c.primary, fontWeight = FontWeight.Bold)
                Box(
                    Modifier
                        .width(32.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(c.primary.copy(alpha = .35f))
                )
                Text(subject.endTime, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(subject.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val details = listOf(subject.teacher, subject.room.takeIf { it.isNotBlank() }?.let { "Ruang $it" }).filterNotNull().filter { it.isNotBlank() }
                Text(details.joinToString(" · ").ifBlank { "Ketuk untuk mengedit detail" }, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Hapus jadwal", tint = c.onSurfaceVariant) }
        }
    }
}

@Composable
private fun UniformSectionEnhanced(uniforms: List<UniformPlan>, onEditDay: (String) -> Unit) {
    RexLifeSectionTitle("Seragam setiap hari", "Atur pakaian dan perlengkapan yang perlu dibawa.")
    RexLifeDays.forEachIndexed { index, day ->
        val plan = uniforms.firstOrNull { it.day == day }
        val c = MaterialTheme.colorScheme

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(400, delayMillis = index * 50, easing = EaseOutCubic),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(400, delayMillis = index * 50))
        ) {
            RexLifeInteractiveCard(onClick = { onEditDay(day) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.tertiary.copy(alpha = .13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Checkroom, null, tint = c.tertiary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(plan?.outfit?.takeIf { it.isNotBlank() } ?: "Belum diatur", style = MaterialTheme.typography.bodyMedium, color = if (plan == null) c.onSurfaceVariant else c.onSurface)
                        if (!plan?.items.isNullOrBlank()) Text(plan!!.items, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit $day", tint = c.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HomeworkSectionEnhanced(
    homework: List<Homework>,
    onAdd: () -> Unit,
    onEdit: (Homework) -> Unit,
    onDelete: (Homework) -> Unit,
    onToggleDone: (Homework) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("pending") }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Tugas dan PR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${homework.count { !it.done }} belum selesai · ${homework.count { it.done }} selesai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text("Tambah PR")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        FilterChip(selected = filter == "pending", onClick = { filter = "pending" }, label = { Text("Belum selesai") })
        FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("Semua") })
        FilterChip(selected = filter == "done", onClick = { filter = "done" }, label = { Text("Selesai") })
    }
    val shown = homework
        .filter { when (filter) { "done" -> it.done; "all" -> true; else -> !it.done } }
        .sortedWith(compareBy<Homework> { it.deadlineDate }.thenBy { it.deadlineTime })
    if (shown.isEmpty()) {
        RexLifeEmptyState(
            title = when (filter) { "done" -> "Belum ada PR selesai"; "all" -> "Belum ada PR"; else -> "Semua tugas beres" },
            description = "Tambahkan PR dengan tombol di atas. Notifikasi belum termasuk versi awal.",
            icon = Icons.Default.Book,
            actionLabel = if (filter == "pending") "Tambah PR Hari Ini" else null,
            onAction = if (filter == "pending") onAdd else null
        )
    } else {
        shown.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = true,
                enter = getCardEntranceAnimation(index),
                exit = getCardExitAnimation(index)
            ) {
                HomeworkCardEnhanced(
                    homework = item,
                    onClick = { onEdit(item) },
                    onDelete = { onDelete(item) },
                    onToggleDone = { onToggleDone(item) }
                )
            }
        }
    }
}

@Composable
private fun HomeworkCompactCardEnhanced(homework: Homework) {
    val c = MaterialTheme.colorScheme
    val urgentPulse = homework.priority == 2

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = c.surfaceVariant.copy(alpha = .54f),
        border = BorderStroke(1.dp, if (urgentPulse) c.error.copy(alpha = 0.5f) else c.outlineVariant.copy(alpha = .6f))
    ) {
        PulseAnimation(isActive = urgentPulse) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background((if (urgentPulse) c.error else c.primary).copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Book, null, tint = if (urgentPulse) c.error else c.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(homework.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (urgentPulse) Surface(color = c.error.copy(alpha = 0.15f), shape = CircleShape) {
                            Text("URGENT", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = c.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(homework.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${formatDateDisplay(homework.deadlineDate)} · ${homework.deadlineTime}", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HomeworkCardEnhanced(
    homework: Homework,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: () -> Unit
) {
    val c = MaterialTheme.colorScheme

    RexLifeInteractiveCard(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = homework.done, onCheckedChange = { onToggleDone() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(homework.subject, style = MaterialTheme.typography.labelMedium, color = c.primary, fontWeight = FontWeight.Bold)
                Text(homework.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val metadata = buildList {
                    if (homework.pages.isNotBlank()) add(homework.pages)
                    add("${formatDateDisplay(homework.deadlineDate)} · ${homework.deadlineTime}")
                }
                Text(metadata.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                if (homework.done) Text("Selesai", style = MaterialTheme.typography.labelSmall, color = c.primary)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Hapus PR", tint = c.onSurfaceVariant) }
        }
    }
}

@Composable
private fun SubjectEditorDialog(
    initial: SchoolSubject?,
    onDismiss: () -> Unit,
    onSave: (SchoolSubject) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var day by remember(initial) { mutableStateOf(initial?.day ?: RexLifeDays.first()) }
    var startTime by remember(initial) { mutableStateOf(initial?.startTime ?: "07:00") }
    var endTime by remember(initial) { mutableStateOf(initial?.endTime ?: "07:45") }
    var teacher by remember(initial) { mutableStateOf(initial?.teacher.orEmpty()) }
    var room by remember(initial) { mutableStateOf(initial?.room.orEmpty()) }
    var dayExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah jadwal pelajaran" else "Edit jadwal") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 510.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Mata pelajaran *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Box {
                    OutlinedTextField(day, {}, label = { Text("Hari") }, readOnly = true, modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dayExpanded = true })
                    DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        RexLifeDays.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                day = option
                                dayExpanded = false
                            })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Mulai", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = {
                            val parts = startTime.split(":")
                            TimePickerDialog(context, { _, h, m -> startTime = "%02d:%02d".format(Locale.US, h, m) },
                                parts.getOrNull(0)?.toIntOrNull() ?: 7, parts.getOrNull(1)?.toIntOrNull() ?: 0, true).show()
                        }, modifier = Modifier.fillMaxWidth()) { Text(startTime) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Selesai", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = {
                            val parts = endTime.split(":")
                            TimePickerDialog(context, { _, h, m -> endTime = "%02d:%02d".format(Locale.US, h, m) },
                                parts.getOrNull(0)?.toIntOrNull() ?: 7, parts.getOrNull(1)?.toIntOrNull() ?: 45, true).show()
                        }, modifier = Modifier.fillMaxWidth()) { Text(endTime) }
                    }
                }
                OutlinedTextField(teacher, { teacher = it }, label = { Text("Guru (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(room, { room = it }, label = { Text("Ruang kelas (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    name.isBlank() -> error = "Nama mata pelajaran wajib diisi."
                    !isValidTime(startTime) || !isValidTime(endTime) -> error = "Jam harus dalam format HH:mm."
                    endTime <= startTime -> error = "Jam selesai harus setelah jam mulai."
                    else -> onSave(SchoolSubject(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(), day = day, startTime = startTime, endTime = endTime,
                        teacher = teacher.trim(), room = room.trim()
                    ))
                }
            }) { Text("Simpan jadwal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun UniformEditorDialog(
    initial: UniformPlan?,
    onDismiss: () -> Unit,
    onSave: (UniformPlan) -> Unit
) {
    var day by remember(initial) { mutableStateOf(initial?.day ?: RexLifeDays.first()) }
    var outfit by remember(initial) { mutableStateOf(initial?.outfit.orEmpty()) }
    var items by remember(initial) { mutableStateOf(initial?.items.orEmpty()) }
    var dayExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jadwal seragam") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    OutlinedTextField(day, {}, label = { Text("Hari") }, readOnly = true, modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dayExpanded = true })
                    DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        RexLifeDays.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                day = option
                                dayExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(outfit, { outfit = it }, label = { Text("Jenis seragam *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Contoh: putih abu-abu") })
                OutlinedTextField(items, { items = it }, label = { Text("Atribut dan perlengkapan") }, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("Dasi, topi, ikat pinggang...") })
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (outfit.isBlank()) error = "Jenis seragam wajib diisi."
                else onSave(UniformPlan(
                    id = initial?.id ?: UUID.randomUUID().toString(),
                    day = day, outfit = outfit.trim(), items = items.trim()
                ))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun HomeworkEditorDialog(
    initial: Homework?,
    scheduledSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (Homework) -> Unit
) {
    var manualSubject by remember(initial, scheduledSubjects) {
        mutableStateOf(scheduledSubjects.isEmpty() || (initial != null && initial.subject !in scheduledSubjects))
    }
    var subject by remember(initial, scheduledSubjects) {
        mutableStateOf(initial?.subject ?: scheduledSubjects.firstOrNull().orEmpty())
    }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var pages by remember(initial) { mutableStateOf(initial?.pages.orEmpty()) }
    var date by remember(initial) { mutableStateOf(initial?.deadlineDate ?: todayIso()) }
    var time by remember(initial) { mutableStateOf(initial?.deadlineTime ?: "23:59") }
    var priority by remember(initial) { mutableStateOf(initial?.priority ?: 1) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah PR Hari Ini" else "Edit PR") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (scheduledSubjects.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Mata pelajaran", style = MaterialTheme.typography.labelLarge)
                            Text(
                                if (manualSubject) "Input manual" else "Ambil dari jadwal pelajaran",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = {
                            manualSubject = !manualSubject
                            if (!manualSubject && subject !in scheduledSubjects) subject = scheduledSubjects.first()
                        }) { Text(if (manualSubject) "Pilih jadwal" else "Input manual") }
                    }
                } else {
                    Text(
                        "Belum ada mata pelajaran di jadwal. Masukkan nama secara manual atau buat jadwal terlebih dahulu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (manualSubject) {
                    OutlinedTextField(subject, { subject = it }, label = { Text("Mata pelajaran *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                } else {
                    Box {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih mata pelajaran *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { subjectExpanded = true },
                            trailingIcon = { Icon(Icons.Default.School, null) }
                        )
                        DropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                            scheduledSubjects.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    subject = option
                                    subjectExpanded = false
                                })
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    description, { description = it },
                    label = { Text("Deskripsi PR *") },
                    placeholder = { Text("Contoh: Kerjakan soal teks eksplanasi") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    pages, { pages = it },
                    label = { Text("Halaman / nomor soal (opsional)") },
                    placeholder = { Text("Contoh: Hal. 35–38, nomor 1–5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("Prioritas", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = priority == 1, onClick = { priority = 1 }, label = { Text("Normal") })
                    FilterChip(selected = priority == 2, onClick = { priority = 2 }, label = { Text("URGENT") })
                }
                
                Text("Tanggal pengumpulan", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = { showNativeDatePicker(context, date) { date = it } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(formatDateDisplay(date))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Jam pengumpulan", style = MaterialTheme.typography.labelLarge)
                        Text(time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        val parts = time.split(":")
                        TimePickerDialog(context, { _, h, m -> time = "%02d:%02d".format(Locale.US, h, m) },
                            parts.getOrNull(0)?.toIntOrNull() ?: 23, parts.getOrNull(1)?.toIntOrNull() ?: 59, true).show()
                    }) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Atur jam")
                    }
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    subject.isBlank() -> error = "Mata pelajaran wajib diisi."
                    description.isBlank() -> error = "Deskripsi PR wajib diisi."
                    !isValidIsoDate(date) -> error = "Tanggal pengumpulan tidak valid."
                    !isValidTime(time) -> error = "Jam pengumpulan tidak valid."
                    else -> onSave(Homework(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        subject = subject.trim(),
                        description = description.trim(),
                        pages = pages.trim(),
                        deadlineDate = date,
                        deadlineTime = time,
                        done = initial?.done ?: false,
                        priority = priority
                    ))
                }
            }) { Text("Simpan PR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun showNativeDatePicker(
    context: android.content.Context,
    value: String,
    onSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
        if (parsed != null) calendar.time = parsed
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onSelected("%04d-%02d-%02d".format(Locale.US, year, month + 1, day))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}


private fun isValidTime(value: String): Boolean {
    if (!value.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) return false
    return true
}

private fun isValidIsoDate(value: String): Boolean {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    return runCatching {
        val parsed = parser.parse(value.trim()) ?: return false
        parser.format(parsed) == value.trim()
    }.getOrDefault(false)
}

private fun formatDateDisplay(value: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val date = runCatching { parser.parse(value) }.getOrNull() ?: return value
    return SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID")).format(date)
}
