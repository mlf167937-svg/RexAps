package com.rexaps.rexlife

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

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

    BackHandler(enabled = showTransactionDialog || showGoalDialog || deletingTransaction != null || deletingGoal != null) {
        showTransactionDialog = false
        showGoalDialog = false
        deletingTransaction = null
        deletingGoal = null
    }

    val data = store.data
    val balance = data.transactions.sumOf { if (it.kind == "expense") -it.amount else it.amount }
    val totalIncome = data.transactions.filter { it.kind != "expense" }.sumOf { it.amount }
    val totalExpense = data.transactions.filter { it.kind == "expense" }.sumOf { it.amount }
    val recentTransactions = data.transactions.sortedWith(compareByDescending<SavingsTransaction> { it.date }.thenByDescending { it.id })

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        RexLifePageHeader("MySavings", "Catat uang dan pantau target tabungan.", onBack)
        SavingsBalanceCard(
            balance = balance,
            income = totalIncome,
            expense = totalExpense
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    defaultKind = "income"
                    editingTransaction = null
                    showTransactionDialog = true
                },
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
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
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Pengeluaran", maxLines = 1)
            }
        }
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
                data.goals.forEach { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onEdit = { editingGoal = goal; showGoalDialog = true },
                        onDelete = { deletingGoal = goal }
                    )
                }
            }
        }
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
                recentTransactions.forEach { transaction ->
                    SavingsTransactionCard(
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
            text = { Text("Transaksi “${target.title}” akan dihapus dari riwayat.") },
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
            text = { Text("Target “${target.title}” akan dihapus.") },
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
private fun SavingsBalanceCard(balance: Long, income: Long, expense: Long) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(c.primary, c.tertiary)))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TOTAL SALDO", style = MaterialTheme.typography.labelMedium, color = c.onPrimary.copy(alpha = .80f))
                Text(formatRupiah(balance), style = MaterialTheme.typography.headlineMedium, color = c.onPrimary, fontWeight = FontWeight.ExtraBold)
            }
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(c.onPrimary.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Savings, null, tint = c.onPrimary, modifier = Modifier.size(25.dp))
            }
        }
        Divider(color = c.onPrimary.copy(alpha = .20f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.ArrowDownward, null, tint = c.onPrimary.copy(alpha = .82f), modifier = Modifier.size(15.dp))
                    Text("Uang masuk", style = MaterialTheme.typography.labelMedium, color = c.onPrimary.copy(alpha = .82f))
                }
                Text(formatRupiah(income), style = MaterialTheme.typography.titleSmall, color = c.onPrimary, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.ArrowUpward, null, tint = c.onPrimary.copy(alpha = .82f), modifier = Modifier.size(15.dp))
                    Text("Pengeluaran", style = MaterialTheme.typography.labelMedium, color = c.onPrimary.copy(alpha = .82f))
                }
                Text(formatRupiah(expense), style = MaterialTheme.typography.titleSmall, color = c.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(goal: SavingsGoal, onEdit: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val progress = if (goal.targetAmount > 0L) (goal.savedAmount.toFloat() / goal.targetAmount).coerceIn(0f, 1f) else 0f
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = c.surface),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = .75f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrackChanges, null, tint = c.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${formatRupiah(goal.savedAmount)} dari ${formatRupiah(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit target", tint = c.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Hapus target", tint = c.error) }
            }
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(progress * 100).toInt()}% tercapai", style = MaterialTheme.typography.labelMedium, color = c.primary)
                Text("Sisa ${formatRupiah((goal.targetAmount - goal.savedAmount).coerceAtLeast(0L))}", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SavingsTransactionCard(
    transaction: SavingsTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val income = transaction.kind != "expense"
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = c.surface),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = .7f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background((if (income) c.primary else c.error).copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(if (income) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (income) c.primary else c.error)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(transaction.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(transaction.category, transaction.date).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${if (income) "+" else "−"}${formatRupiah(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (income) c.primary else c.error,
                    fontWeight = FontWeight.Bold
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
    var error by remember { mutableStateOf<String?>(null) }
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah transaksi" else "Edit transaksi") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "income", onClick = { kind = "income" }, label = { Text("Uang masuk") })
                    FilterChip(selected = kind == "expense", onClick = { kind = "expense" }, label = { Text("Pengeluaran") })
                }
                OutlinedTextField(title, { title = it }, label = { Text("Nama transaksi") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(amountText, { amountText = it.filter(Char::isDigit) }, label = { Text("Nominal (Rp)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(category, { category = it }, label = { Text("Kategori (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Tanggal (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                    date = date.trim()
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
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Target tabungan baru" else "Edit target") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Nama target") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(targetText, { targetText = it.filter(Char::isDigit) }, label = { Text("Nominal target (Rp)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(savedText, { savedText = it.filter(Char::isDigit) }, label = { Text("Sudah terkumpul (Rp)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Nilai yang sudah terkumpul dapat diperbarui kapan saja dengan mengedit target.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    savedAmount = saved
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
