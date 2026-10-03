package com.rexaps.rexpanel

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
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

private enum class PanelTab(
    val label: String,
    val icon: ImageVector
) {
    Dashboard("Dasbor", Icons.Default.Dashboard),
    Terminal("Terminal", Icons.Default.Terminal),
    Files("File", Icons.Default.Folder)
}

@Composable
fun RexPanelScreen(
    activity: Activity,
    onExit: () -> Unit
) {
    val owner =
        activity as? ViewModelStoreOwner
            ?: error(
                "RexPanelScreen requires an Activity that implements ViewModelStoreOwner"
            )

    val vm =
        ViewModelProvider(owner)[
            RexPanelViewModel::class.java
        ]

    val colors = MaterialTheme.colorScheme

    var fullscreen by rememberSaveable {
        mutableStateOf(false)
    }

    var fontSize by rememberSaveable {
        mutableIntStateOf(12)
    }

    var tabIdx by rememberSaveable {
        mutableIntStateOf(0)
    }

    val state = vm.conn
    val files = vm.files

    val tab =
        PanelTab.values()[tabIdx]

    val connected =
        state is ConnState.Connected

    val immersive =
        fullscreen ||
            (
                tab == PanelTab.Files &&
                    files?.preview != null
                )

    BackHandler {
        when {
            // Jika sedang memilih file, tombol Back cukup
            // membatalkan selection terlebih dahulu.
            connected &&
                tab == PanelTab.Files &&
                files?.selecting == true -> {
                files.clearSelection()
            }

            fullscreen -> {
                fullscreen = false
            }

            connected &&
                tab == PanelTab.Files &&
                files?.preview != null -> {
                files.preview = null
            }

            connected &&
                tab == PanelTab.Files &&
                files?.canGoUp == true -> {
                files.up()
            }

            connected &&
                tab != PanelTab.Dashboard -> {
                tabIdx = 0
            }

            else -> {
                onExit()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        if (state is ConnState.Connected) {

            Column(
                Modifier.fillMaxSize()
            ) {

                if (!immersive) {

                    PanelHeader(
                        target = state.target,
                        host = vm.metrics?.host,
                        onMinimize = onExit,
                        onDisconnect = {
                            vm.disconnect()
                        }
                    )

                    PanelTabs(tab) {
                        tabIdx = it.ordinal
                    }
                }

                when (tab) {

                    PanelTab.Dashboard -> {

                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(
                                    rememberScrollState()
                                ),
                            contentAlignment =
                                Alignment.TopCenter
                        ) {
                            Column(
                                Modifier.widthIn(
                                    max = 720.dp
                                )
                            ) {
                                MonitorSection(
                                    vm.metrics,
                                    vm.cpuHistory
                                )
                            }
                        }
                    }

                    PanelTab.Terminal -> {

                        TerminalPane(
                            vm = vm,
                            fontSize = fontSize,
                            fullscreen = fullscreen,
                            onToggleFull = {
                                fullscreen = !fullscreen
                            },
                            onFontSize = {
                                fontSize = it
                            },
                            modifier =
                                Modifier.weight(1f)
                        )

                        KeyBar(vm)
                    }

                    PanelTab.Files -> {

                        if (files != null) {
                            FilesScreen(
                                files,
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

        } else {

            val ctx = LocalContext.current

            ConnectForm(
                state = state,

                // Penting:
                // ViewModel sekarang menerima Context,
                // bukan File/cacheDir.
                onConnect = {
                        cmd,
                        pw,
                        elevate ->
                    vm.connect(
                        cmd,
                        pw,
                        elevate,
                        ctx.applicationContext
                    )
                },

                onExit = onExit
            )
        }
    }
}

/* -------------------------------- CONNECT -------------------------------- */

@Composable
private fun ConnectForm(
    state: ConnState,
    onConnect: (
        String,
        String,
        Boolean
    ) -> Unit,
    onExit: () -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    val context =
        LocalContext.current

    val prefs =
        remember {
            context.getSharedPreferences(
                "rexpanel",
                Context.MODE_PRIVATE
            )
        }

    var command by rememberSaveable(
        stateSaver = TextFieldValue.Saver
    ) {
        val t =
            prefs
                .getString(
                    "last_cmd",
                    ""
                )
                .orEmpty()

        mutableStateOf(
            TextFieldValue(
                t,
                TextRange(t.length)
            )
        )
    }

    var password by remember {
        mutableStateOf("")
    }

    var showPw by remember {
        mutableStateOf(false)
    }

    var elevate by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean(
                "elevate",
                false
            )
        )
    }

    val busy =
        state is ConnState.Connecting

    val accent =
        Brush.linearGradient(
            listOf(
                colors.primary,
                colors.tertiary
            )
        )

    fun preset(t: String) {
        command =
            TextFieldValue(
                t,
                TextRange(t.length)
            )
    }

    fun submit() {
        if (busy) return

        prefs
            .edit()
            .putString(
                "last_cmd",
                command.text.trim()
            )
            .putBoolean(
                "elevate",
                elevate
            )
            .apply()

        onConnect(
            command.text,
            password,
            elevate
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 24.dp,
                vertical = 12.dp
            )
    ) {

        IconButton(
            onClick = onExit
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "Kembali"
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        Box(
            Modifier
                .size(64.dp)
                .clip(
                    RoundedCornerShape(20.dp)
                )
                .background(accent),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                ">_",
                color = colors.onPrimary,
                fontWeight =
                    FontWeight.Bold,
                fontFamily =
                    FontFamily.Monospace,
                fontSize = 22.sp
            )
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "RexPanel",
            style =
                MaterialTheme.typography
                    .headlineLarge
        )

        Text(
            "Monitor, terminal, dan file server lewat SSH. Tidak perlu root.",
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                colors.onSurfaceVariant
        )

        Spacer(
            Modifier.height(24.dp)
        )

        Row(
            Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            PresetChip(
                "Termux · 8022"
            ) {
                preset(
                    "ssh -p 8022 user@192.168."
                )
            }

            PresetChip(
                "Server Linux · 22"
            ) {
                preset(
                    "ssh user@"
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = command,
            onValueChange = {
                command = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text("Perintah SSH")
            },
            placeholder = {
                Text(
                    "ssh -p 8022 u0_a123@192.168.0.101"
                )
            },
            supportingText = {
                Text(
                    "User wajib ditulis. Di Termux biasanya u0_aXXX (cek dengan whoami)."
                )
            },
            singleLine = true,
            shape =
                RoundedCornerShape(20.dp),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Uri,
                    imeAction =
                        ImeAction.Next
                )
        )

        Spacer(
            Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            singleLine = true,
            visualTransformation =
                if (showPw) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {

                IconButton(
                    onClick = {
                        showPw = !showPw
                    }
                ) {
                    Icon(
                        if (showPw) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        if (showPw) {
                            "Sembunyikan password"
                        } else {
                            "Tampilkan password"
                        }
                    )
                }
            },
            shape =
                RoundedCornerShape(20.dp),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password,
                    imeAction =
                        ImeAction.Go
                ),
            keyboardActions =
                KeyboardActions(
                    onGo = {
                        submit()
                    }
                )
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .toggleable(
                    value = elevate,
                    role = Role.Switch,
                    onValueChange = {
                        elevate = it
                    }
                )
                .padding(
                    vertical = 8.dp,
                    horizontal = 4.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    "Izinkan sudo/su",
                    style =
                        MaterialTheme.typography
                            .bodyLarge
                )

                Text(
                    "Opsional. Hanya untuk membaca data sistem yang butuh izin lebih. Biarkan mati untuk akun biasa.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        colors.onSurfaceVariant
                )
            }

            Spacer(
                Modifier.width(12.dp)
            )

            Switch(
                checked = elevate,
                onCheckedChange = null
            )
        }

        if (state is ConnState.Error) {

            Spacer(
                Modifier.height(8.dp)
            )

            Row(
                Modifier.semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                },
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = colors.error,
                    modifier =
                        Modifier.size(18.dp)
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    state.message,
                    color = colors.error,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .alpha(
                    if (busy) 0.6f else 1f
                )
                .semantics(
                    mergeDescendants = true
                ) {
                    role = Role.Button

                    if (busy) {
                        disabled()
                    }
                }
                .rexPressable(
                    enabled = !busy
                ) {
                    submit()
                }
                .clip(CircleShape)
                .background(accent),
            contentAlignment =
                Alignment.Center
        ) {

            if (busy) {

                CircularProgressIndicator(
                    Modifier.size(22.dp),
                    color =
                        colors.onPrimary,
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    "Hubungkan",
                    color =
                        colors.onPrimary,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "Password tidak disimpan. Verifikasi host key belum aktif di versi ini.",
            style =
                MaterialTheme.typography
                    .labelSmall,
            color =
                colors.onSurfaceVariant
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(12.dp)

    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .border(
                1.dp,
                colors.outlineVariant,
                shape
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = 14.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            label,
            style =
                MaterialTheme.typography
                    .labelLarge
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
    val colors =
        MaterialTheme.colorScheme

    var confirm by remember {
        mutableStateOf(false)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onMinimize
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "Kembali"
            )
        }

        Column(
            Modifier.weight(1f)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            Good,
                            CircleShape
                        )
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    host ?: target.host,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    "Terhubung",
                    style =
                        MaterialTheme.typography
                            .labelSmall,
                    color =
                        colors.onSurfaceVariant
                )
            }

            Text(
                "${target.user}@${target.host}:${target.port}",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    colors.onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = {
                confirm = true
            }
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                "Putuskan koneksi",
                tint = colors.error
            )
        }
    }

    if (confirm) {

        AlertDialog(
            onDismissRequest = {
                confirm = false
            },

            title = {
                Text(
                    "Putuskan koneksi?"
                )
            },

            text = {
                Text(
                    "Sesi terminal akan ditutup dan perintah yang sedang berjalan bisa berhenti."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        confirm = false
                        onDisconnect()
                    }
                ) {
                    Text(
                        "Putuskan",
                        color = colors.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        confirm = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun PanelTabs(
    selected: PanelTab,
    onSelect: (PanelTab) -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 6.dp
            )
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(
                colors.surfaceVariant
            )
            .padding(4.dp)
            .selectableGroup(),

        horizontalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        PanelTab.values().forEach { t ->

            val sel =
                t == selected

            val fg =
                if (sel) {
                    colors.onPrimary
                } else {
                    colors.onSurfaceVariant
                }

            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        if (sel) {
                            colors.primary
                        } else {
                            Color.Transparent
                        }
                    )
                    .selectable(
                        selected = sel,
                        role = Role.Tab,
                        onClick = {
                            onSelect(t)
                        }
                    )
                    .padding(
                        horizontal = 8.dp
                    ),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    t.icon,
                    null,
                    Modifier.size(18.dp),
                    tint = fg
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text(
                    t.label,
                    style =
                        MaterialTheme.typography
                            .labelLarge,
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

/* -------------------------------- MONITOR -------------------------------- */

@Composable
private fun MonitorSection(
    m: Metrics?,
    cpuHistory: List<Float>
) {
    Column(
        Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        if (m == null) {

            LinearProgressIndicator(
                Modifier.fillMaxWidth()
            )

            Text(
                "Mengambil data server…",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }

        val total =
            m?.memTotalKb

        val used =
            m?.memUsedKb

        val memFrac =
            if (
                total != null &&
                used != null &&
                total > 0
            ) {
                used.toFloat() /
                    total
            } else {
                null
            }

        val cpuFrac =
            m?.cpu ?: m?.cpuFreqFrac

        val cpuValue =
            when {
                m?.cpu != null ->
                    "${(m.cpu * 100).toInt()}%"

                m?.cpuFreqMhz != null ->
                    fmtMhz(m.cpuFreqMhz)

                else ->
                    "–"
            }

        val coreText =
            m?.cores?.let {
                " · $it core"
            } ?: ""

        val cpuSub =
            when {
                m?.cpu != null ->
                    "Penggunaan$coreText"

                m?.cpuFreqMhz != null ->
                    "Frekuensi$coreText"

                else ->
                    "Tidak tersedia"
            }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            GaugeCard(
                title = "CPU",
                fraction = cpuFrac,
                value = cpuValue,
                sub = cpuSub,
                history = cpuHistory,
                modifier =
                    Modifier.weight(1f)
            )

            GaugeCard(
                title = "RAM",
                fraction = memFrac,
                value =
                    memFrac?.let {
                        "${(it * 100).toInt()}%"
                    } ?: "–",
                sub =
                    if (
                        used != null &&
                        total != null
                    ) {
                        "${fmtKb(used)} / ${fmtKb(total)}"
                    } else {
                        "Tidak tersedia"
                    },
                history = null,
                modifier =
                    Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            StatTile(
                "Disk ${m?.diskMount ?: ""}".trim(),
                m?.diskPct?.let {
                    "$it%"
                } ?: "–",
                if (
                    m?.diskUsedKb != null &&
                    m.diskTotalKb != null
                ) {
                    "${fmtKb(m.diskUsedKb)} / ${fmtKb(m.diskTotalKb)}"
                } else {
                    ""
                },
                Modifier.weight(1f)
            )

            StatTile(
                "Uptime",
                m?.uptimeSec?.let {
                    fmtUptime(it)
                } ?: m?.uptimeText ?: "–",
                "",
                Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            StatTile(
                "Load",
                m?.load
                    ?.substringBefore(' ')
                    ?: "–",
                m?.load ?: "",
                Modifier.weight(1f)
            )

            StatTile(
                "Jaringan",
                "↓ " +
                    (
                        m?.rxBps?.let {
                            fmtRate(it)
                        } ?: "–"
                        ),
                "↑ " +
                    (
                        m?.txBps?.let {
                            fmtRate(it)
                        } ?: "–"
                        ),
                Modifier.weight(1f)
            )
        }

        if (m?.batteryPct != null) {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                StatTile(
                    "Baterai",
                    "${m.batteryPct}%",
                    m.batteryStatus
                        ?.lowercase()
                        ?.replaceFirstChar {
                            it.uppercase()
                        } ?: "",
                    Modifier.weight(1f)
                )

                StatTile(
                    "Suhu",
                    m.batteryTemp?.let {
                        String.format(
                            "%.1f°C",
                            it
                        )
                    } ?: "–",
                    "Baterai",
                    Modifier.weight(1f)
                )
            }
        }

        if (
            m != null &&
            m.cpu == null &&
            m.rxBps == null
        ) {

            Text(
                "Sebagian data tidak tersedia karena akun ini bukan root. Itu normal; terminal dan file tetap berfungsi.",
                style =
                    MaterialTheme.typography
                        .labelSmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

private fun fmtMhz(
    mhz: Int
): String =
    if (mhz >= 1000) {
        String.format(
            "%.1f GHz",
            mhz / 1000.0
        )
    } else {
        "$mhz MHz"
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
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(24.dp)

    val f =
        (fraction ?: 0f)
            .coerceIn(0f, 1f)

    val sweep by animateFloatAsState(
        f * 270f,
        tween(700),
        label = "gauge"
    )

    val arcColor =
        when {
            f < 0.6f ->
                colors.primary

            f < 0.85f ->
                Warn

            else ->
                Bad
        }

    val level =
        when {
            fraction == null ->
                "tidak tersedia"

            f < 0.6f ->
                "normal"

            f < 0.85f ->
                "tinggi"

            else ->
                "kritis"
        }

    val track =
        colors.outlineVariant

    val lineColor =
        colors.tertiary

    Column(
        modifier
            .clip(shape)
            .background(
                colors.surfaceVariant
            )
            .border(
                1.dp,
                colors.outlineVariant,
                shape
            )
            .padding(14.dp)
            .semantics(
                mergeDescendants = true
            ) {
                contentDescription =
                    "$title $value, $level. $sub"
            },

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            title,
            style =
                MaterialTheme.typography
                    .labelMedium,
            color =
                colors.onSurfaceVariant
        )

        Spacer(
            Modifier.height(6.dp)
        )

        Box(
            Modifier.size(100.dp),
            contentAlignment =
                Alignment.Center
        ) {

            Canvas(
                Modifier.fillMaxSize()
            ) {

                val stroke =
                    10.dp.toPx()

                val arcSize =
                    Size(
                        size.width - stroke,
                        size.height - stroke
                    )

                val topLeft =
                    Offset(
                        stroke / 2,
                        stroke / 2
                    )

                drawArc(
                    track,
                    135f,
                    270f,
                    false,
                    topLeft,
                    arcSize,
                    style =
                        Stroke(
                            stroke,
                            cap =
                                StrokeCap.Round
                        )
                )

                if (sweep > 0.5f) {

                    drawArc(
                        arcColor,
                        135f,
                        sweep,
                        false,
                        topLeft,
                        arcSize,
                        style =
                            Stroke(
                                stroke,
                                cap =
                                    StrokeCap.Round
                            )
                    )
                }
            }

            Text(
                value,
                style =
                    MaterialTheme.typography
                        .titleLarge
            )
        }

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            sub,
            style =
                MaterialTheme.typography
                    .labelSmall,
            color =
                colors.onSurfaceVariant,
            maxLines = 1
        )

        if (
            history != null &&
            history.size >= 2
        ) {

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(28.dp)
            ) {

                val path =
                    Path()

                history.forEachIndexed {
                    i,
                    v ->

                    val x =
                        size.width *
                            i /
                            (history.size - 1)

                    val y =
                        size.height *
                            (
                                1f -
                                    v.coerceIn(
                                        0f,
                                        1f
                                    )
                                )

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path,
                    lineColor,
                    style =
                        Stroke(
                            2.dp.toPx(),
                            cap =
                                StrokeCap.Round
                        )
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier
) {
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(20.dp)

    Column(
        modifier
            .clip(shape)
            .background(
                colors.surfaceVariant
            )
            .border(
                1.dp,
                colors.outlineVariant,
                shape
            )
            .padding(14.dp),

        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {

        Text(
            title,
            style =
                MaterialTheme.typography
                    .labelMedium,
            color =
                colors.onSurfaceVariant
        )

        Text(
            value,
            style =
                MaterialTheme.typography
                    .titleMedium,
            color =
                colors.primary,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )

        if (sub.isNotEmpty()) {

            Text(
                sub,
                style =
                    MaterialTheme.typography
                        .labelSmall,
                color =
                    colors.onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

private fun fmtKb(
    kb: Long
): String {
    val mb =
        kb / 1024.0

    return if (mb >= 1024) {
        String.format(
            "%.1f GB",
            mb / 1024
        )
    } else {
        String.format(
            "%.0f MB",
            mb
        )
    }
}

private fun fmtRate(
    bps: Long
): String =
    when {
        bps >= 1_048_576 ->
            String.format(
                "%.1f MB/s",
                bps / 1_048_576.0
            )

        bps >= 1024 ->
            String.format(
                "%.0f KB/s",
                bps / 1024.0
            )

        else ->
            "$bps B/s"
    }

private fun fmtUptime(
    sec: Long
): String {

    val d =
        sec / 86_400

    val h =
        sec % 86_400 / 3600

    val m =
        sec % 3600 / 60

    return when {
        d > 0 ->
            "${d}h ${h}j"

        h > 0 ->
            "${h}j ${m}m"

        else ->
            "${m}m"
    }
}

/* -------------------------------- TERMINAL -------------------------------- */

@Composable
private fun TerminalPane(
    vm: RexPanelViewModel,
    fontSize: Int,
    fullscreen: Boolean,
    onToggleFull: () -> Unit,
    onFontSize: (Int) -> Unit,
    modifier: Modifier
) {
    val colors =
        MaterialTheme.colorScheme

    val density =
        LocalDensity.current

    val measurer =
        rememberTextMeasurer()

    val style =
        remember(fontSize) {
            TextStyle(
                fontFamily =
                    FontFamily.Monospace,
                fontSize =
                    fontSize.sp,
                lineHeight =
                    (fontSize * 1.35f).sp
            )
        }

    val cell =
        remember(style) {
            measurer.measure(
                "W",
                style
            ).size
        }

    val rowH =
        with(density) {
            cell.height.toDp()
        }

    val termBg =
        lerp(
            colors.background,
            Color.Black,
            0.55f
        )

    val focus =
        remember {
            FocusRequester()
        }

    val keyboard =
        LocalSoftwareKeyboardController
            .current

    val term =
        vm.term

    val version =
        term.version

    Column(modifier) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                "Terminal",
                style =
                    MaterialTheme.typography
                        .titleSmall,
                modifier =
                    Modifier.weight(1f)
            )

            TextButton(
                onClick = {
                    onFontSize(
                        (
                            fontSize - 1
                            ).coerceAtLeast(8)
                    )
                }
            ) {
                Text("A−")
            }

            TextButton(
                onClick = {
                    onFontSize(
                        (
                            fontSize + 1
                            ).coerceAtMost(22)
                    )
                }
            ) {
                Text("A+")
            }

            IconButton(
                onClick = onToggleFull
            ) {

                Icon(
                    if (fullscreen) {
                        Icons.Default.FullscreenExit
                    } else {
                        Icons.Default.Fullscreen
                    },
                    if (fullscreen) {
                        "Keluar fullscreen"
                    } else {
                        "Fullscreen"
                    }
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal =
                        if (fullscreen) {
                            0.dp
                        } else {
                            12.dp
                        }
                )
                .clip(
                    RoundedCornerShape(
                        if (fullscreen) {
                            0.dp
                        } else {
                            20.dp
                        }
                    )
                )
                .background(termBg)
                .clipToBounds()
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = null
                ) {
                    focus.requestFocus()
                    keyboard?.show()
                }
                .padding(8.dp)
                .onSizeChanged { size ->

                    val c =
                        size.width /
                            cell.width

                    val r =
                        size.height /
                            cell.height

                    if (
                        c >= 10 &&
                        r >= 3
                    ) {
                        vm.resize(
                            c,
                            r
                        )
                    }
                }
        ) {

            Column {

                for (
                    r in 0 until term.rows
                ) {

                    Text(
                        text =
                            term.rowText(
                                version,
                                r,
                                TermFg,
                                colors.primary,
                                termBg
                            ),
                        style = style,
                        color = TermFg,
                        softWrap = false,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Clip,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(rowH)
                    )
                }
            }

            // Input tersembunyi untuk
            // menangkap keyboard.
            BasicTextField(
                value = "",
                onValueChange = {
                    if (it.isNotEmpty()) {
                        vm.sendText(it)
                    }
                },
                modifier =
                    Modifier
                        .size(1.dp)
                        .alpha(0f)
                        .focusRequester(focus)
                        .onPreviewKeyEvent {
                            handleKey(
                                it,
                                vm
                            )
                        },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Text,
                        imeAction =
                            ImeAction.Send
                    ),
                keyboardActions =
                    KeyboardActions(
                        onSend = {
                            vm.sendRaw(
                                "\r"
                            )
                        }
                    )
            )
        }
    }
}

private fun handleKey(
    e: KeyEvent,
    vm: RexPanelViewModel
): Boolean {

    if (
        e.type !=
            KeyEventType.KeyDown
    ) {
        return false
    }

    when (e.key) {

        Key.Enter,
        Key.NumPadEnter ->
            vm.sendRaw("\r")

        Key.Backspace ->
            vm.sendRaw("\u007F")

        Key.Tab ->
            vm.sendRaw("\t")

        Key.Escape ->
            vm.sendRaw("\u001B")

        Key.DirectionUp ->
            vm.sendCursor('A')

        Key.DirectionDown ->
            vm.sendCursor('B')

        Key.DirectionRight ->
            vm.sendCursor('C')

        Key.DirectionLeft ->
            vm.sendCursor('D')

        else ->
            return false
    }

    return true
}

/* --------------------------------- KEYBAR --------------------------------- */

@Composable
private fun KeyBar(
    vm: RexPanelViewModel
) {
    val colors =
        MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .background(
                colors.surfaceVariant
            )
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),

        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        KeyChip(
            "Ctrl",
            vm.ctrl
        ) {
            vm.toggleCtrl()
        }

        KeyChip(
            "Alt",
            vm.alt
        ) {
            vm.toggleAlt()
        }

        KeyChip(
            "Shift",
            vm.shift
        ) {
            vm.toggleShift()
        }

        KeyChip("Esc") {
            vm.sendRaw("\u001B")
        }

        KeyChip("Tab") {
            vm.sendRaw("\t")
        }

        KeyChip("←") {
            vm.sendCursor('D')
        }

        KeyChip("↑") {
            vm.sendCursor('A')
        }

        KeyChip("↓") {
            vm.sendCursor('B')
        }

        KeyChip("→") {
            vm.sendCursor('C')
        }

        KeyChip("Home") {
            vm.sendCursor('H')
        }

        KeyChip("End") {
            vm.sendCursor('F')
        }

        KeyChip("PgUp") {
            vm.sendTilde(5)
        }

        KeyChip("PgDn") {
            vm.sendTilde(6)
        }

        KeyChip("Del") {
            vm.sendTilde(3)
        }

        KeyChip("^C") {
            vm.sendRaw("\u0003")
        }

        KeyChip("^D") {
            vm.sendRaw("\u0004")
        }

        KeyChip("^Z") {
            vm.sendRaw("\u001A")
        }

        KeyChip("-") {
            vm.sendText("-")
        }

        KeyChip("/") {
            vm.sendText("/")
        }

        KeyChip("|") {
            vm.sendText("|")
        }

        KeyChip("~") {
            vm.sendText("~")
        }
    }
}

@Composable
private fun KeyChip(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    val shape =
        RoundedCornerShape(12.dp)

    Box(
        Modifier
            .height(44.dp)
            .widthIn(min = 44.dp)
            .semantics {
                role = Role.Button
                selected = active
            }
            .rexPressable(
                onClick = onClick
            )
            .clip(shape)
            .background(
                if (active) {
                    colors.primary
                } else {
                    colors.background
                }
            )
            .border(
                1.dp,
                if (active) {
                    colors.primary
                } else {
                    colors.outlineVariant
                },
                shape
            )
            .padding(
                horizontal = 12.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            label,
            style =
                MaterialTheme.typography
                    .labelLarge,
            color =
                if (active) {
                    colors.onPrimary
                } else {
                    colors.onSurface
                }
        )
    }
}