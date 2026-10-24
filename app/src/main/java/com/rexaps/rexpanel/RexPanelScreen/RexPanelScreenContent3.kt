package com.rexaps.rexpanel

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

@Composable internal
fun __RexPanelScreenContent(
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
            .background(colors.background.copy(alpha = 0.93f))
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
