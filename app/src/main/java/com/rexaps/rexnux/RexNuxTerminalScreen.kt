package com.rexaps.rexnux

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexNuxTerminalScreen(
    distro: RexNuxDistro,
    onBack: () -> Unit,
    viewModel: RexNuxTerminalViewModel = viewModel()
) {
    val output by viewModel.output
    val input by viewModel.input
    val scroll = rememberScrollState()
    val toolScroll = rememberScrollState()

    LaunchedEffect(distro) {
        viewModel.start(distro)
    }

    LaunchedEffect(output) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    DisposableEffect(distro) {
        onDispose {
            viewModel.stop()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F141C),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF4AF626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "RexNux • ${distro.title}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "PRoot ARM64 • /files/home",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearScreen() }) {
                        Icon(
                            Icons.Default.Clear,
                            "Clear Screen",
                            tint = Color.LightGray
                        )
                    }
                    IconButton(onClick = { viewModel.start(distro) }) {
                        Icon(
                            Icons.Default.Refresh,
                            "Restart Terminal",
                            tint = Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161E2E)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .padding(padding)
        ) {
            // Area Display Output Terminal (Monospace)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Color(0xFF05080E), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = output,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll),
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Termux-Style Accessory Key Row (Ctrl, Tab, Esc, Arrows, Shortcuts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131B2A))
                    .horizontalScroll(toolScroll)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccessoryKeyButton("ESC") { viewModel.sendSpecialKey("ESC") }
                AccessoryKeyButton("TAB") { viewModel.sendSpecialKey("TAB") }
                AccessoryKeyButton("Ctrl+C") { viewModel.sendControlKey('c') }
                AccessoryKeyButton("Ctrl+D") { viewModel.sendControlKey('d') }
                AccessoryKeyButton("Ctrl+Z") { viewModel.sendControlKey('z') }
                AccessoryKeyButton("Ctrl+L") { viewModel.clearScreen() }
                AccessoryKeyButton("▲") { viewModel.historyPrev() }
                AccessoryKeyButton("▼") { viewModel.historyNext() }
                AccessoryKeyButton("ls -la") {
                    viewModel.setInput("ls -la")
                    viewModel.execute()
                }
                AccessoryKeyButton("uname -a") {
                    viewModel.setInput("uname -a")
                    viewModel.execute()
                }
                AccessoryKeyButton("id") {
                    viewModel.setInput("id")
                    viewModel.execute()
                }
            }

            // Command Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = viewModel::setInput,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Ketik perintah Linux...",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.execute() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = Color(0xFF4AF626)
                    )
                )

                Spacer(Modifier.width(6.dp))

                Button(
                    onClick = viewModel::execute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7)
                    )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Run Command",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessoryKeyButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E293B)
    ) {
        Text(
            text = label,
            color = Color(0xFF93C5FD),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
