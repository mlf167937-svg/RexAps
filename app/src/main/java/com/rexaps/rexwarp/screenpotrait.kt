package com.rexaps.rexwarp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rexaps.ui.LocalRexScreenLayoutMode
import com.rexaps.ui.RexScreenLayoutMode
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun RexWarpScreenPortrait(
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
    viewModel: RexWarpViewModel = viewModel()
) {
    CompositionLocalProvider(LocalRexScreenLayoutMode provides RexScreenLayoutMode.PORTRAIT) {
        RexWarpScreenContent(modifier, onExit, viewModel)
    }
}
