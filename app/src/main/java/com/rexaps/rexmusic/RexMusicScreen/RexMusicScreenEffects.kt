package com.rexaps.rexmusic

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Composable
internal fun RexMusicScreenEffects(
    vm: RexMusicViewModel,
    query: String,
    onTypingChange: (Boolean) -> Unit,
    snackbar: SnackbarHostState
) {
    LaunchedEffect(vm) {
        vm.refreshOffline()
    }

    LaunchedEffect(vm) {
        vm.state
            .map { it.notice }
            .distinctUntilChanged()
            .filterNotNull()
            .collectLatest { message ->
                vm.consumeNotice()
                snackbar.showSnackbar(message)
            }
    }

    LaunchedEffect(query) {
        val typing = query.isNotBlank()
        onTypingChange(typing)
        if (typing) {
            delay(450)
            onTypingChange(false)
        }
    }
}
