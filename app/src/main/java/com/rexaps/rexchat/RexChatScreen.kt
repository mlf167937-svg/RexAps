package com.rexaps.rexchat

import android.app.Activity
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.rexaps.rexfox.rexPressable

@Composable
fun RexChatScreen(
    activity: Activity,
    onExit: () -> Unit
) {

    /*
     * ---------------------------------------------------------
     * VIEWMODEL
     * ---------------------------------------------------------
     */

    val owner =
        activity as? ViewModelStoreOwner
            ?: error(
                "RexChatScreen requires a ViewModelStoreOwner"
            )

    val vm =
        ViewModelProvider(
            owner,
            RexChatViewModel.Factory(activity)
        )[RexChatViewModel::class.java]

    val colors =
        MaterialTheme.colorScheme

    /*
     * ---------------------------------------------------------
     * LOCAL UI STATE
     * ---------------------------------------------------------
     */

    var showMenu by remember {
        mutableStateOf(false)
    }

    var confirmClear by remember {
        mutableStateOf(false)
    }

    /*
     * ---------------------------------------------------------
     * FILE PICKER
     * ---------------------------------------------------------
     */

    val filePicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetMultipleContents()
        ) { uris ->

            vm.onFilesPicked(
                uris
            )
        }

    /*
     * ---------------------------------------------------------
     * CAMERA / MICROPHONE
     * ---------------------------------------------------------
     */

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val granted =
                result.isNotEmpty() &&
                        result.values.all {
                            it
                        }

            vm.onPermissionsResult(
                granted
            )
        }

    /*
     * ---------------------------------------------------------
     * CALLBACK BINDING
     * ---------------------------------------------------------
     */

    DisposableEffect(vm) {

        vm.onPickFiles = {

            filePicker.launch(
                "*/*"
            )
        }

        vm.onNeedPermissions = { permissions ->

            permissionLauncher.launch(
                permissions
            )
        }

        onDispose {

            vm.onPickFiles = null
            vm.onNeedPermissions = null

            /*
             * Jangan destroy WebView di sini.
             *
             * ViewModel masih memegang session.
             */
            vm.flush()
        }
    }

    /*
     * ---------------------------------------------------------
     * BACK BUTTON
     * ---------------------------------------------------------
     */

    BackHandler {

        if (vm.canGoBack()) {

            vm.goBack()

        } else {

            onExit()
        }
    }

    /*
     * ---------------------------------------------------------
     * ROOT
     * ---------------------------------------------------------
     */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        /*
         * =====================================================
         * HEADER
         * =====================================================
         */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * BACK
             */

            IconButton(
                onClick = {

                    if (vm.canGoBack()) {
                        vm.goBack()
                    } else {
                        onExit()
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription =
                        "Kembali"
                )
            }

            /*
             * ICON
             */

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary,
                                colors.tertiary
                            )
                        )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Chat,
                    contentDescription =
                        null,
                    tint =
                        colors.onPrimary,
                    modifier =
                        Modifier.size(21.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            /*
             * TITLE
             */

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "RexChat",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    text =
                        if (
                            vm.pageTitle.isBlank() ||
                            vm.pageTitle == "WhatsApp Web"
                        ) {
                            "WhatsApp Web"
                        } else {
                            vm.pageTitle
                        },
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        colors.onSurfaceVariant,
                    maxLines = 1
                )
            }

            /*
             * RELOAD
             */

            IconButton(
                onClick = {
                    vm.reload()
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Refresh,
                    contentDescription =
                        "Muat ulang"
                )
            }

            /*
             * MENU
             */

            Box {

                IconButton(
                    onClick = {
                        showMenu = true
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.MoreVert,
                        contentDescription =
                            "Menu"
                    )
                }

                DropdownMenu(
                    expanded =
                        showMenu,
                    onDismissRequest = {
                        showMenu = false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text(
                                "Hapus sesi"
                            )
                        },
                        leadingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Logout,
                                contentDescription =
                                    null
                            )
                        },
                        onClick = {

                            showMenu = false

                            confirmClear = true
                        }
                    )
                }
            }
        }

        /*
         * =====================================================
         * PROGRESS
         * =====================================================
         */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {

            if (vm.loading) {

                LinearProgressIndicator(
                    progress = {
                        vm.progress
                            .coerceIn(
                                0,
                                100
                            ) / 100f
                    },
                    modifier =
                        Modifier.fillMaxSize(),
                    color =
                        colors.tertiary,
                    trackColor =
                        Color.Transparent
                )
            }
        }

        /*
         * =====================================================
         * WEBVIEW AREA
         * =====================================================
         */

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            /*
             * -------------------------------------------------
             * WEBVIEW
             * -------------------------------------------------
             */

            AndroidView(
                factory = {

                    /*
                     * Biasanya tidak diperlukan,
                     * tetapi ini mencegah crash jika WebView
                     * masih punya parent lama.
                     */
                    (
                        vm.webView.parent
                            as? ViewGroup
                        )?.removeView(
                            vm.webView
                        )

                    vm.webView
                },
                update = {
                    /*
                     * Tidak perlu melakukan apa-apa.
                     *
                     * Jangan memanggil loadUrl() di sini,
                     * karena AndroidView update dipanggil
                     * berkali-kali saat Compose recomposition.
                     */
                },
                modifier =
                    Modifier.fillMaxSize()
            )

            /*
             * -------------------------------------------------
             * ERROR OVERLAY
             * -------------------------------------------------
             */

            vm.error?.let { message ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            colors.background
                        )
                        .padding(32.dp),
                    verticalArrangement =
                        Arrangement.Center,
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    /*
                     * ERROR ICON
                     */

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                colors.surfaceVariant,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                colors.outlineVariant,
                                CircleShape
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.WifiOff,
                            contentDescription =
                                null,
                            tint =
                                colors.tertiary,
                            modifier =
                                Modifier.size(32.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    /*
                     * MESSAGE
                     */

                    Text(
                        text = message,
                        textAlign =
                            TextAlign.Center,
                        color =
                            colors.onSurfaceVariant,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    /*
                     * RETRY
                     */

                    Box(
                        modifier = Modifier
                            .rexPressable {
                                vm.reload()
                            }
                            .clip(
                                CircleShape
                            )
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        colors.primary,
                                        colors.tertiary
                                    )
                                )
                            )
                            .padding(
                                horizontal = 28.dp,
                                vertical = 12.dp
                            )
                    ) {

                        Text(
                            text = "Coba lagi",
                            color =
                                colors.onPrimary,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                    }
                }
            }
        }
    }

    /*
     * =========================================================
     * CLEAR SESSION DIALOG
     * =========================================================
     */

    if (confirmClear) {

        AlertDialog(
            onDismissRequest = {
                confirmClear = false
            },

            title = {
                Text(
                    "Hapus sesi?"
                )
            },

            text = {
                Text(
                    "Sesi WhatsApp Web di RexChat akan " +
                            "dihapus. Setelah itu kamu perlu " +
                            "menautkan perangkat lagi."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        confirmClear = false

                        vm.clearSession()
                    }
                ) {

                    Text(
                        "Hapus"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        confirmClear = false
                    }
                ) {

                    Text(
                        "Batal"
                    )
                }
            }
        )
    }
}
