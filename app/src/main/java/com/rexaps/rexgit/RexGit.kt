package com.rexaps.rexgit

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RexGit() {
    // Construct this AndroidViewModel explicitly. This avoids relying on a
    // default ViewModel factory that may try a reflective zero-argument
    // constructor in a minified Release build.
    val application = LocalContext.current.applicationContext as Application
    val factory = remember(application) {
        RexGitViewModelFactory(application)
    }
    val viewModel: RexGitViewModel = viewModel(factory = factory)

    RexGitTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RexGitScreen(viewModel = viewModel)
        }
    }
}

private class RexGitViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != RexGitViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return RexGitViewModel(application) as T
    }
}
