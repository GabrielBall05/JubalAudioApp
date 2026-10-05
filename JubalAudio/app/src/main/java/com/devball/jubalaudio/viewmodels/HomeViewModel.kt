package com.devball.jubalaudio.viewmodels

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.data.local.entity.MediaEntity
import com.devball.jubalaudio.data.local.entity.PlaylistEntity
import com.devball.jubalaudio.data.repository.MediaRepository
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.data.repository.UserPreferencesRepository
import com.devball.jubalaudio.delegates.MediaActionsDelegate
import com.devball.jubalaudio.delegates.PlaylistActionsDelegate
import com.devball.jubalaudio.delegates.SearchDelegate
import com.devball.jubalaudio.delegates.SelectionDelegate
import com.devball.jubalaudio.utilgen.MediaSortOrder
import com.devball.jubalaudio.viewmodels.events.UiEvent
import com.devball.jubalaudio.utilgen.getCommonArtwork
import com.devball.jubalaudio.utilgen.getCommonCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val searchDelegate: SearchDelegate,
    private val selectionDelegate: SelectionDelegate,
    private val mediaActionsDelegate: MediaActionsDelegate,
    private val playlistActionsDelegate: PlaylistActionsDelegate
): BaseViewModel(),
    SearchDelegate by searchDelegate,
    SelectionDelegate by selectionDelegate,
    MediaActionsDelegate by mediaActionsDelegate,
    PlaylistActionsDelegate by playlistActionsDelegate {

    //Media Sort Order
    val sortOrder: StateFlow<MediaSortOrder> = userPreferencesRepository.mediaSortOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.INITIAL_MEDIA_SORT_ORDER)

    //Full Media List
    private val _allMedia = mediaRepository.allMedia

    //Filtered list by combining with the search query - list used for UI
    val filteredMedia = combine(_allMedia, searchQuery, sortOrder) { media, query, sort ->
        //Filter first
        val filtered = if (query.isBlank()) { //Search field empty, show whole list
            media
        } else { //Only show list where title or creator contains the search query (case insensitive)
            media.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.creator.contains(query, ignoreCase = true)
            }
        }
        //Then sort
        when (sort) {
            MediaSortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            MediaSortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            MediaSortOrder.CREATOR_ASC -> filtered.sortedBy { it.creator.lowercase() }
            MediaSortOrder.CREATOR_DESC -> filtered.sortedByDescending { it.creator.lowercase() }
            MediaSortOrder.DURATION_ASC -> filtered.sortedBy { it.duration }
            MediaSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.duration }
            MediaSortOrder.DATE_ADDED_MOST_RECENT -> filtered.sortedByDescending { it.dateAdded }
            MediaSortOrder.DATE_ADDED_LEAST_RECENT -> filtered.sortedBy { it.dateAdded }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //Full list filtered by staleUri
    val allStaleMedia = _allMedia.map { list ->
        list.filter { it.isStaleUri }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    //Simple flag for whether the user has media or not
    val hasMedia: StateFlow<Boolean> = _allMedia
        .map { list -> list.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    init {
        //Bind delegates to the ViewModel's lifecycle and UI handler
        bindSelectionScope(viewModelScope)
        bindMediaActions(this)
        bindPlaylistActions(this)

        //Validate URI integrity and notify user if applicable
        viewModelScope.launch {
            scanUris()
        }
    }


    fun onSortOrderChange(newOrder: MediaSortOrder) = launchWithoutLoading {
        userPreferencesRepository.updateMediaSortOrder(newOrder)
    }

    //Override SearchDelegate's onSearchQueryChange because changing search query should also clear selection
    override fun onSearchQueryChange(newQuery: String) {
        searchDelegate.onSearchQueryChange(newQuery)
        selectionDelegate.clearSelection()
    }

    //Override MediaActionsDelegate's deleteMediaByIds because deleting should also remove them from selection
    override fun deleteMediaByIds(ids: List<Int>) {
        mediaActionsDelegate.deleteMediaByIds(ids)
        selectionDelegate.removeSelections(ids)
    }

    fun toggleSelectAll() {
        toggleSelectAll(filteredMedia.value
            .filter { !it.isStaleUri }
            .map { it.mediaId }
        )
    }

    fun importMedia(uriList: List<Uri>) = launchWithLoading {
        try {
            val newIds = mediaRepository.importMedia(uriList)
            showToast("Added ${newIds.size} item${if (newIds.size > 1) "s" else ""} to library" )
            selectionDelegate.addSelections(newIds)
        } catch (e: Exception) {
            showToast("Failed to import media")
        }
    }

    suspend fun scanUris() {
        val numStale = mediaRepository.validateMediaUris()
        if (numStale > 0) showToast(
            message = "$numStale media have invalid file paths. View in Home screen via filter option.",
            length = Toast.LENGTH_LONG
        )
    }
}