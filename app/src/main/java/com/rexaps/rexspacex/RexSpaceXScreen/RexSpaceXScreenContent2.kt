package com.rexaps.rexspacex

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable internal
fun __RexSpaceXScreenContent(onExit: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedMode by rememberSaveable { mutableStateOf("BALANCED") }
    var fpsStabilizer by rememberSaveable { mutableStateOf(true) }
    var touchBoost by rememberSaveable { mutableStateOf(true) }
    var doNotDisturb by rememberSaveable { mutableStateOf(false) }
    var overlayHint by rememberSaveable { mutableStateOf(false) }
    var sensitivity by rememberSaveable { mutableFloatStateOf(0.65f) }

    val modes = listOf("SAVE", "BALANCED", "EXTREME")
    val accent by animateColorAsState(
        targetValue = if (selectedMode == "EXTREME") SpaceRed else SpaceBlue,
        label = "spaceXAccent"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SpacePanel.copy(alpha = 0.76f), SpaceBlack.copy(alpha = 0.86f), SpaceBlack.copy(alpha = 0.91f))
                )
            )
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                RexSpaceXHeader(onExit)
            }

            item {
                RexSpaceXHero(selectedMode, accent)
            }

            item {
                RexSpaceXProfile(modes, selectedMode, accent, onModeSelect)
            }

            item {
                RexSpaceXQuickTuning(fpsStabilizer, touchBoost, doNotDisturb, sensitivity, accent, { fpsStabilizer = it }, { touchBoost = it }, { doNotDisturb = it }, { sensitivity = it })
            }

            item {
                RexSpaceXQuickLaunch(accent)
            }
            item {
                RexSpaceXGamingTools(accent)
            }
            item {
                RexSpaceXCoreStatus()
            }
    }
}
}
