package com.devball.jubalaudio.delegates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface SearchDelegate {
    val searchQuery: StateFlow<String>
    fun onSearchQueryChange(newQuery: String)
}

class SearchDelegateImpl @Inject constructor() : SearchDelegate {
    private val _searchQuery = MutableStateFlow("")
    override val searchQuery = _searchQuery.asStateFlow()

    override fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
}