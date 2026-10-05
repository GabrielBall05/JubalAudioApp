package com.devball.jubalaudio.delegates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface SelectionDelegate {
    val selectedMediaIds: StateFlow<List<Int>>
    val isAnySelected: StateFlow<Boolean>
    fun toggleSelection(id: Int)
    fun addSelections(ids: List<Int>)
    fun removeSelections(ids: List<Int>)
    fun clearSelection()
    fun toggleSelectAll(allValidIds: List<Int>)
    fun bindSelectionScope(scope: CoroutineScope)
}

class SelectionDelegateImpl @Inject constructor() : SelectionDelegate {
    private val _selectedMediaIds = MutableStateFlow<List<Int>>(emptyList())
    override val selectedMediaIds = _selectedMediaIds.asStateFlow()

    private val _isAnySelected = MutableStateFlow(false)
    override val isAnySelected = _isAnySelected.asStateFlow()

    override fun bindSelectionScope(scope: CoroutineScope) {
        _selectedMediaIds.map { it.isNotEmpty() }
            .onEach { _isAnySelected.value = it }
            .launchIn(scope)
    }

    override fun toggleSelection(id: Int) {
        _selectedMediaIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    override fun addSelections(ids: List<Int>) {
        _selectedMediaIds.update { current -> (current + ids).distinct() }
    }

    override fun removeSelections(ids: List<Int>) {
        _selectedMediaIds.update { current -> current - ids.toSet() }
    }

    override fun toggleSelectAll(allValidIds: List<Int>) {
        _selectedMediaIds.value = if (_selectedMediaIds.value.size == allValidIds.size) emptyList() else allValidIds
    }

    override fun clearSelection() {
        _selectedMediaIds.value = emptyList()
    }
}