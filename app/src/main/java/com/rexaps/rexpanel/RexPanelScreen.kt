package com.rexaps.rexpanel

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.rexaps.rexfox.rexPressable

private val TermFg = Color(0xFFCDD6F4)
private val Good = Color(0xFFA6E3A1)
private val Warn = Color(0xFFF9E2AF)
private val Bad = Color(0xFFF38BA8)

@Composable
fun RexPanelScreen(activity: Activity, onExit: () -> Unit) {
    val owner = activity as? ViewModelStoreOwner
        ?: error("RexPanelScreen requires an Activity that implements ViewModelStoreOwner")
    val vm = ViewModelProvider(owner)[RexPanelViewModel::class.java]
    val colors = MaterialTheme.colorScheme

    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableIntStateOf(12) }
    var termHeight by rememberSaveable { mutableFloatStateOf(320f) }

    BackHandler { if (fullscreen) fullscreen = false else onExit() }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val state = vm.conn
        if (state is ConnState.Connected) {
            Column(Modifier.fillMaxSize()) {
                if (!fullscreen) {
                    PanelHeader(
                        target = state.target,
                        host = vm.metrics?.host,
                        onMinimize = onExit,
                        onDisconnect = { vm.disconnect() }
                    )
                }

                if (fullscreen) {
                    TerminalPane(
                        vm = vm,
                        fontSize = fontSize,
                        fullscreen = true,
                        onToggleFull = { fullscreen = false },
                        onFontSize = { fontSize = it },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MonitorSection(vm.metrics, vm.cpuHistory)
                        DragHandle { dy -> termHeight = (termHeight + dy).coerceIn(160f, 720f) }
                        TerminalPane(
                            vm = vm,
                            fontSize = fontSize,
                            fullscreen = false,
                            onToggleFull = { fullscreen = true },
                            onFontSize = { fontSize = it },
                            modifier = Modifier.height(termHeight.dp)
                        )
                    }
                }

                KeyBar(vm)
            }
        } else {
            ConnectForm(
                state = state,
                onConnect = { cmd, pw -> vm.connect(cmd, pw) },
                onExit = onExit
            )
        }
    }
}

/* -------------------------------- CONNECT -------------------------------- */

@Composable
private fun ConnectForm(
    state: ConnState,
    onConnect: (String, String) -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var command by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val busy = state is ConnState.Connecting
    val accent = Brush.linearGradient(listOf(colors.primary, colors.tertiary))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        IconButton(onClick = onExit) { Icon(Icons.Default.ArrowBack, "Kembali") }
        Spacer(Modifier.height(24.dp))

        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(">_", color = colors.onPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 22.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("RexPanel", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Monitoring dan kontrol server lewat SSH.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Perintah SSH") },
            placeholder = { Text("ssh -p 8022 root@192.168.0.101") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (!busy) onConnect(command, password) })
        )

        if (state is ConnState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(state.message, color = colors.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .alpha(if (busy) 0.6f else 1f)
                .rexPressable(enabled = !busy) { onConnect(command, password) }
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(22.dp), color = colors.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("Hubungkan", color = colors.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Password tidak disimpan. Verifikasi host key belum aktif di versi ini.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

/* -------------------------------- HEADER --------------------------------- */

@Composable
private fun PanelHeader(
    target: SshTarget,
    host: String?,
    onMinimize: () -> Unit,
    onDisconnect: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMinimize) { Icon(Icons.Default.ArrowBack, "Kembali") }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Good, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(host ?: target.host, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${target.user}@${target.host}:${target.port}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        IconButton(onClick = onDisconnect) {
            Icon(Icons.Default.PowerSettingsNew, "Putuskan koneksi", tint = colors.error)
        }
    }
}

/* -------------------------------- MONITOR -------------------------------- */

@Composable
private fun MonitorSection(m: Metrics?, cpuHistory: List<Float>) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val total = m?.memTotalKb
        val used = m?.memUsedKb
        val memFrac = if (total != null && used != null && total > 0) used.toFloat() / total else null

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GaugeCard(
                title = "CPU",
                fraction = m?.cpu,
                value = m?.cpu?.let { "${(it * 100).toInt()}%" } ?: "–",
                sub = if (m?.cpu == null) "Tidak tersedia" else "Penggunaan",
                history = cpuHistory,
                modifier = Modifier.weight(1f)
            )
            GaugeCard(
                title = "RAM",
                fraction = memFrac,
                value = memFrac?.let { "${(it * 100).toInt()}%" } ?: "–",
                sub = if (used != null && total != null) "${fmtKb(used)} / ${fmtKb(total)}" else "Tidak tersedia",
                history = null,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                "Disk /",
                m?.diskPct?.let { "$it%" } ?: "–",
                if (m?.diskUsedKb != null && m.diskTotalKb != null) "${fmtKb(m.diskUsedKb)} / ${fmtKb(m.diskTotalKb)}" else "",
                Modifier.weight(1f)
            )
            StatTile("Uptime", m?.uptimeSec?.let { fmtUptime(it) } ?: "–", "", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Load", m?.load?.substringBefore(' ') ?: "–", m?.load ?: "", Modifier.weight(1f))
            StatTile(
                "Jaringan",
                "↓ " + (m?.rxBps?.let { fmtRate(it) } ?: "–"),
                "↑ " + (m?.txBps?.let { fmtRate(it) } ?: "–"),
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GaugeCard(
    title: String,
    fraction: Float?,
    value: String,
    sub: String,
    history: List<Float>?,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val f = (fraction ?: 0f).coerceIn(0f, 1f)
    val sweep by animateFloatAsState(f * 270f, tween(700), label = "gauge")
    val arcColor = when {
        f < 0.6f -> colors.primary
        f < 0.85f -> Warn
        else -> Bad
    }
    val track = colors.outlineVariant
    val lineColor = colors.tertiary

    Column(
        modifier
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (sweep > 0.5f) {
                    drawArc(arcColor, 135f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(4.dp))
        Text(sub, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)

        if (history != null && history.size >= 2) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(28.dp)
            ) {
                val path = Path()
                history.forEachIndexed { i, v ->
                    val x = size.width * i / (history.size - 1)
                    val y = size.height * (1f - v.coerceIn(0f, 1f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, sub: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub.isNotEmpty()) {
            Text(sub, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun fmtKb(kb: Long): String {
    val mb = kb / 1024.0
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
}

private fun fmtRate(bps: Long): String = when {
    bps >= 1_048_576 -> String.format("%.1f MB/s", bps / 1_048_576.0)
    bps >= 1024 -> String.format("%.0f KB/s", bps / 1024.0)
    else -> "$bps B/s"
}

private fun fmtUptime(sec: Long): String {
    val d = sec / 86_400
    val h = sec % 86_400 / 3600
    val m = sec % 3600 / 60
    return when {
        d > 0 -> "${d}h ${h}j"
        h > 0 -> "${h}j ${m}m"
        else -> "${m}m"
    }
}

/* -------------------------------- TERMINAL -------------------------------- */

@Composable
private fun DragHandle(onDrag: (Float) -> Unit) {
    val density = LocalDensity.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> onDrag(delta / density.density) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(44.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
    }
}

@Composable
private fun TerminalPane(
    vm: RexPanelViewModel,
    fontSize: Int,
    fullscreen: Boolean,
    onToggleFull: () -> Unit,
    onFontSize: (Int) -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = remember(fontSize) {
        TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, lineHeight = (fontSize * 1.35f).sp)
    }
    val cell = remember(style) { measurer.measure("W", style).size }
    val rowH = with(density) { cell.height.toDp() }
    val termBg = lerp(colors.background, Color.Black, 0.55f)
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val term = vm.term
    val version = term.version

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Terminal", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { onFontSize((fontSize - 1).coerceAtLeast(8)) }) { Text("A−") }
            TextButton(onClick = { onFontSize((fontSize + 1).coerceAtMost(22)) }) { Text("A+") }
            IconButton(onClick = onToggleFull) {
                Icon(
                    if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    if (fullscreen) "Keluar fullscreen" else "Fullscreen"
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = if (fullscreen) 0.dp else 12.dp)
                .clip(RoundedCornerShape(if (fullscreen) 0.dp else 20.dp))
                .background(termBg)
                .clipToBounds()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focus.requestFocus()
                    keyboard?.show()
                }
                .padding(8.dp)
                .onSizeChanged { size ->
                    val c = size.width / cell.width
                    val r = size.height / cell.height
                    if (c >= 10 && r >= 3) vm.resize(c, r)
                }
        ) {
            Column {
                for (r in 0 until term.rows) {
                    Text(
                        text = term.rowText(version, r, TermFg, colors.primary, termBg),
                        style = style,
                        color = TermFg,
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowH)
                    )
                }
            }

            // Input tersembunyi untuk menangkap keyboard.
            BasicTextField(
                value = "",
                onValueChange = { if (it.isNotEmpty()) vm.sendText(it) },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focus)
                    .onPreviewKeyEvent { handleKey(it, vm) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.VisiblePassword, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.sendRaw("\r") })
            )
        }
    }
}

private fun handleKey(e: KeyEvent, vm: RexPanelViewModel): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    when (e.key) {
        Key.Enter, Key.NumPadEnter -> vm.sendRaw("\r")
        Key.Backspace -> vm.sendRaw("\u007F")
        Key.Tab -> vm.sendRaw("\t")
        Key.Escape -> vm.sendRaw("\u001B")
        Key.DirectionUp -> vm.sendCursor('A')
        Key.DirectionDown -> vm.sendCursor('B')
        Key.DirectionRight -> vm.sendCursor('C')
        Key.DirectionLeft -> vm.sendCursor('D')
        else -> return false
    }
    return true
}

/* --------------------------------- KEYBAR --------------------------------- */

@Composable
private fun KeyBar(vm: RexPanelViewModel) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        KeyChip("Ctrl", vm.ctrl) { vm.toggleCtrl() }
        KeyChip("Alt", vm.alt) { vm.toggleAlt() }
        KeyChip("Shift", vm.shift) { vm.toggleShift() }
        KeyChip("Esc") { vm.sendRaw("\u001B") }
        KeyChip("Tab") { vm.sendRaw("\t") }
        KeyChip("←") { vm.sendCursor('D') }
        KeyChip("↑") { vm.sendCursor('A') }
        KeyChip("↓") { vm.sendCursor('B') }
        KeyChip("→") { vm.sendCursor('C') }
        KeyChip("Home") { vm.sendCursor('H') }
        KeyChip("End") { vm.sendCursor('F') }
        KeyChip("PgUp") { vm.sendTilde(5) }
        KeyChip("PgDn") { vm.sendTilde(6) }
        KeyChip("Del") { vm.sendTilde(3) }
        KeyChip("^C") { vm.sendRaw("\u0003") }
        KeyChip("^D") { vm.sendRaw("\u0004") }
        KeyChip("^Z") { vm.sendRaw("\u001A") }
        KeyChip("-") { vm.sendText("-") }
        KeyChip("/") { vm.sendText("/") }
        KeyChip("|") { vm.sendText("|") }
        KeyChip("~") { vm.sendText("~") }
    }
}

@Composable
private fun KeyChip(label: String, active: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .height(40.dp)
            .widthIn(min = 44.dp)
            .rexPressable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) colors.primary else colors.background)
            .border(1.dp, if (active) colors.primary else colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) colors.onPrimary else colors.onSurface
        )
    }
}
