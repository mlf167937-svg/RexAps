package com.rexaps.rextools

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RexToolsUiState(
    val categories: List<ToolCategory> = RexToolRegistry.categoriesWithTools(),
    val selectedCategory: ToolCategory? = null
)

class RexToolsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RexToolsUiState())
    val uiState: StateFlow<RexToolsUiState> = _uiState.asStateFlow()

    fun selectCategory(category: ToolCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun toolsForSelected(): List<RexTool> {
        val category = _uiState.value.selectedCategory ?: return emptyList()
        return RexToolRegistry.toolsFor(category)
    }
}
