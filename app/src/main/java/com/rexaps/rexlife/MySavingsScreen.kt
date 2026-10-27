package com.rexaps.rexlife

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun MySavingsScreen(
    store: RexLifeStore,
    onBack: () -> Unit
) {
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var defaultKind by rememberSaveable { mutableStateOf("income") }
    var editingTransaction by remember { mutableStateOf<SavingsTransaction?>(null) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deletingTransaction by remember { mutableStateOf<SavingsTransaction?>(null) }
    var deletingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var activeTab by rememberSaveable { mutableStateOf("overview") }

    BackHandler(enabled = showTransactionDialog || showGoalDialog || deletingTransaction != null || deletingGoal != null) {
        showTransactionDialog = false
        showGoalDialog = false
        deletingTransaction = null
        deletingGoal = null
    }

    val data = store.data
    val analytics = data.getAnalytics()
    val balance = data.transactions.sumOf { if (it.kind == "expense") -it.amount else it.amount }
    val totalIncome = analytics.totalIncome
    val totalExpense = analytics.totalExpense
    val recentTransactions = data.transactions.sortedWith(compareByDescending<SavingsTransaction> { it.date }.thenByDescending { it.id })

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        RexLifePageHeader("MySavings", "Analytics, target, dan riwayat transaksi.", onBack)
        
        // Enhanced Balance Card dengan animated number counter
        SavingsBalanceCardEnhanced(
            balance = balance,
            income = totalIncome,
            expense = totalExpense
        )

        // Quick Action Buttons dengan better UX
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    defaultKind = "income"
                    editingTransaction = null
                    showTransactionDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Uang masuk", maxLines = 1)
            }
            OutlinedButton(
                onClick = {
                    defaultKind = "expense"
                    editingTransaction = null
                    showTransactionDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Pengeluaran", maxLines = 1)
            }
        }

        // Analytics Section dengan visual charts
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RexLifeSectionTitle("Analisis Keuangan", "Tren pengeluaran dan insight.")
            
            // Spending by Category dengan animated bars
            if (analytics.topCategories.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pengeluaran per Kategori", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        analytics.topCategories.forEachIndexed { index, stat ->
                            SpendingCategoryBar(stat, index)
                        }
                    }
                }
            }

            // Savings Rate dengan animated progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Laju Tabungan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${String.format("%.1f", analytics.savingsRate)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (analytics.savingsRate > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    SmoothProgressIndicator(
                        progress = (analytics.savingsRate / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (analytics.savingsRate > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (analytics.savingsRate > 0) "Tabungan positif! Pertahankan momentum ini." else "Pengeluaran melebihi pemasukan. Kurangi pengeluaran atau tambah pemasukan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Savings Goals dengan priority indicator
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RexLifeSectionTitle(
                title = "Target tabungan",
                subtitle = "Pantau progres untuk hal yang ingin kamu beli.",
                action = {
                    IconButton(onClick = { editingGoal = null; showGoalDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Buat target")
                    }
                }
            )
            if (data.goals.isEmpty()) {
                RexLifeEmptyState(
                    title = "Buat target pertamamu",
                    description = "Misalnya laptop, perlengkapan sekolah, atau dana darurat.",
                    icon = Icons.Default.TrackChanges,
                    actionLabel = "Buat target",
                    onAction = { editingGoal = null; showGoalDialog = true }
                )
            } else {
                data.goals.sortedByDescending { it.priority }.forEach { goal ->
                    SavingsGoalCardEnhanced(
                        goal = goal,
                        onEdit = { editingGoal = goal; showGoalDialog = true },
                        onDelete = { deletingGoal = goal }
                    )
                }
            }
        }

        // Transaction History dengan animations
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RexLifeSectionTitle(
                title = "Riwayat transaksi",
                subtitle = "${data.transactions.size} transaksi tersimpan"
            )
            if (recentTransactions.isEmpty()) {
                RexLifeEmptyState(
                    title = "Belum ada transaksi",
                    description = "Tambahkan uang masuk atau pengeluaran untuk mulai menghitung saldo.",
                    icon = Icons.Default.History
                )
            } else {
                recentTransactions.take(10).forEachIndexed { index, transaction ->
                    AnimatedVisibility(
                        visible = true,
                        enter = getCardEntranceAnimation(index),
                        exit = getCardExitAnimation(index)
                    ) {
                        SavingsTransactionCardEnhanced(
                            transaction = transaction,
                            onEdit = {
                                editingTransaction = transaction
                                defaultKind = transaction.kind
                                showTransactionDialog = true
                            },
                            onDelete = { deletingTransaction = transaction }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    if (showTransactionDialog) {
        SavingsTransactionDialog(
            initial = editingTransaction,
            defaultKind = defaultKind,
            onDismiss = { showTransactionDialog = false },
            onSave = { saved ->
                store.update { old ->
                    old.copy(transactions = (old.transactions.filterNot { it.id == saved.id } + saved)
                        .sortedWith(compareByDescending<SavingsTransaction> { it.date }.thenByDescending { it.id }))
                }
                showTransactionDialog = false
            }
        )
    }

    if (showGoalDialog) {
        SavingsGoalDialog(
            initial = editingGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { saved ->
                store.update { old -> old.copy(goals = old.goals.filterNot { it.id == saved.id } + saved) }
                showGoalDialog = false
            }
        )
    }

    deletingTransaction?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Hapus transaksi?") },
            text = { Text("Transaksi \"${target.title}\" akan dihapus dari riwayat.") },
            confirmButton = {
                TextButton(onClick = {
                    store.update { old -> old.copy(transactions = old.transactions.filterNot { it.id == target.id }) }
                    deletingTransaction = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingTransaction = null }) { Text("Batal") } }
        )
    }
    deletingGoal?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingGoal = null },
            title = { Text("Hapus target?") },
            text = { Text("Target \"${target.title}\" akan dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    store.update { old -> old.copy(goals = old.goals.filterNot { it.id == target.id }) }
                    deletingGoal = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingGoal = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SavingsBalanceCardEnhanced(balance: Long, income: Long, expense: Long) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(c.primary, c.tertiary)))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedGap(4.dp)) {
                Text("TOTAL SALDO", style = MaterialTheme.typography.labelMedium, color = c.onPrimary.copy(alpha = .80f))
                AnimatedNumberCounter(
                    value = balance,
                    modifier = Modifier.fillMaxWidth(),
                    formatter = { formatRupiah(it) }
                )
            }
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.onPrimary.copy(alpha = .13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Savings, null, tint = c.onPrimary, modifier = Modifier.size(25.dp))
            }
        }
        Divider(color = c.onPrimary.copy(alpha = .20f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedGap(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedGap(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedGap(5.dp)) {
                    Icon(Icons.Default.ArrowDownward, null, tint = c.onPrimary.copy(alpha = .82f), modifier = Modifier.size(15.dp))
                    Text("Uang masuk", style = MaterialTheme.typography.labelMedium, color = c.onPrimary.copy(alpha = .82f))
                }
                AnimatedNumberCounter(value = income, formatter = { formatRupiah(it) })
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedGap(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedGap(5.dp)) {
                    Icon(Icons.Default.ArrowUpward, null, tint = c.onPrimary.copy(alpha = .82f), modifier = Modifier.size(15.dp))
                    Text("Pengeluaran", style = MaterialTheme.typography.labelMedium, color = c.onPrimary.copy(alpha = .82f))
                }
                AnimatedNumberCounter(value = expense, formatter = { formatRupiah(it) })
            }
        }
    }
}

@Composable
private fun SpendingCategoryBar(stat: CategoryStat, index: Int) {
    val c = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedGap(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stat.category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${String.format("%.1f", stat.percentage)}%",
                style = MaterialTheme.typography.labelSmall,
                color = c.primary
            )
        }
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(
                animationSpec = tween(500, delayMillis = index * 100, easing = EaseOutCubic),
                initialOffsetX = { -it }
            ) + fadeIn(animationSpec = tween(500, delayMillis = index * 100))
        ) {
            SmoothProgressIndicator(
                progress = (stat.percentage / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
        Text(
            formatRupiah(stat.amount),
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant
        )
    }
}

@Composable
private fun SavingsGoalCardEnhanced(goal: SavingsGoal, onEdit: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val progress = if (goal.targetAmount > 0L) (goal.savedAmount.toFloat() / goal.targetAmount).coerceIn(0f, 1f) else 0f
    val priorityColor = when (goal.priority) {
        2 -> c.error
        1 -> c.primary
        else -> c.tertiary
    }
    
    RexLifeInteractiveCard(
        onClick = onEdit,
        modifier = Modifier.animateContentSize(animationSpec = tween(300, easing = EaseOutCubic))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedGap(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedGap(10.dp)) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(priorityColor.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrackChanges, null, tint = priorityColor)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedGap(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedGap(6.dp)) {
                        Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (goal.priority == 2) {
                            Surface(color = c.error.copy(alpha = 0.15f), shape = CircleShape) {
                                Text("URGENT", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = c.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("${formatRupiah(goal.savedAmount)} dari ${formatRupiah(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit target", tint = c.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Hapus target", tint = c.error) }
            }
            SmoothProgressIndicator(progress = progress, modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(progress * 100).toInt()}% tercapai", style = MaterialTheme.typography.labelMedium, color = c.primary)
                Text("Sisa ${formatRupiah((goal.targetAmount - goal.savedAmount).coerceAtLeast(0L))}", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SavingsTransactionCardEnhanced(
    transaction: SavingsTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val income = transaction.kind != "expense"

    RexLifeInteractiveCard(onClick = onEdit) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedGap(11.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background((if (income) c.primary else c.error).copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (income) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (income) c.primary else c.error)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedGap(3.dp)) {
                Text(transaction.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(transaction.category, transaction.date).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedGap(4.dp)) {
                AnimatedNumberCounter(
                    value = transaction.amount,
                    formatter = { "${if (income) "+" else "−"}${formatRupiah(it)}" }
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit transaksi", tint = c.onSurfaceVariant, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Hapus transaksi", tint = c.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsTransactionDialog(
    initial: SavingsTransaction?,
    defaultKind: String,
    onDismiss: () -> Unit,
    onSave: (SavingsTransaction) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var amountText by remember(initial) { mutableStateOf(initial?.amount?.toString().orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category.orEmpty()) }
    var date by remember(initial) { mutableStateOf(initial?.date ?: todayIso()) }
    var kind by remember(initial, defaultKind) { mutableStateOf(initial?.kind ?: defaultKind) }
    var isRecurring by remember(initial) { mutableStateOf(initial?.isRecurring ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    val c = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah transaksi" else "Edit transaksi") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedGap(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedGap(8.dp)) {
                    FilterChip(selected = kind == "income", onClick = { kind = "income" }, label = { Text("Uang masuk") })
                    FilterChip(selected = kind == "expense", onClick = { kind = "expense" }, label = { Text("Pengeluaran") })
                }
                OutlinedTextField(title, { title = it }, label = { Text("Nama transaksi") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(amountText, { amountText = it.filter(Char::isDigit) }, label = { Text("Nominal (Rp)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(category, { category = it }, label = { Text("Kategori (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Tanggal (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedGap(8.dp)) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Transaksi berulang?", style = MaterialTheme.typography.labelMedium)
                }
                
                if (error != null) Text(error!!, color = c.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toLongOrNull()
                if (title.isBlank()) error = "Nama transaksi wajib diisi."
                else if (amount == null || amount <= 0L) error = "Nominal harus lebih besar dari nol."
                else if (!isValidIsoDate(date)) error = "Tanggal harus memakai format YYYY-MM-DD."
                else onSave(SavingsTransaction(
                    id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                    title = title.trim(),
                    amount = amount,
                    kind = kind,
                    category = category.trim(),
                    date = date.trim(),
                    isRecurring = isRecurring
                ))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun SavingsGoalDialog(
    initial: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (SavingsGoal) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var targetText by remember(initial) { mutableStateOf(initial?.targetAmount?.toString().orEmpty()) }
    var savedText by remember(initial) { mutableStateOf(initial?.savedAmount?.toString() ?: "0") }
    var priority by remember(initial) { mutableStateOf(initial?.priority ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Target tabungan baru" else "Edit target") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedGap(10.dp)
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Nama target") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(targetText, { targetText = it.filter(Char::isDigit) }, label = { Text("Nominal target (Rp)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(savedText, { savedText = it.filter(Char::isDigit) }, label = { Text("Sudah terkumpul (Rp)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
                Text("Prioritas", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedGap(6.dp)) {
                    FilterChip(selected = priority == 0, onClick = { priority = 0 }, label = { Text("Rendah") })
                    FilterChip(selected = priority == 1, onClick = { priority = 1 }, label = { Text("Normal") })
                    FilterChip(selected = priority == 2, onClick = { priority = 2 }, label = { Text("Urgent") })
                }
                
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val target = targetText.toLongOrNull()
                val saved = savedText.toLongOrNull() ?: 0L
                if (title.isBlank()) error = "Nama target wajib diisi."
                else if (target == null || target <= 0L) error = "Nominal target harus lebih besar dari nol."
                else if (saved < 0L) error = "Nominal terkumpul tidak valid."
                else onSave(SavingsGoal(
                    id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                    title = title.trim(),
                    targetAmount = target,
                    savedAmount = saved,
                    priority = priority
                ))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun isValidIsoDate(value: String): Boolean {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    return runCatching {
        val parsed = parser.parse(value.trim()) ?: return false
        parser.format(parsed) == value.trim()
    }.getOrDefault(false)
}

internal fun formatRupiah(amount: Long): String {
    val sign = if (amount < 0) "- " else ""
    val digits = kotlin.math.abs(amount).toString().reversed().chunked(3).joinToString(".").reversed()
    return "${sign}Rp$digits"
}
