package com.devball.jubalaudio.ui.viewmodels

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.data.local.PlaylistEntity
import com.devball.jubalaudio.data.repository.MediaRepository
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.data.repository.SettingsRepository
import com.devball.jubalaudio.util.MediaSortOrder
import com.devball.jubalaudio.util.UiEvent
import com.devball.jubalaudio.util.getCommonArtwork
import com.devball.jubalaudio.util.getCommonCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    settingsRepository: SettingsRepository
): BaseViewModel() {
    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Sort State
    private val _sortOrder = MutableStateFlow(SettingsRepository.INITIAL_MEDIA_SORT_ORDER)
    val sortOrder = _sortOrder.asStateFlow()

    //Get all media entities from DB using the interactor's shared flow
    private val _allMedia = mediaRepository.allMedia

    //Filter full list by combining with the search query (this is the list shown in UI)
    val filteredMedia = combine(_allMedia, _searchQuery, _sortOrder) { media, query, sort ->
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allStaleMedia = _allMedia.map { list ->
        list.filter { it.isStaleUri }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    //Selection variables
    private val _selectedMediaIds = MutableStateFlow<List<Int>>(emptyList())
    val selectedMediaIds = _selectedMediaIds.asStateFlow()
    val isAnySelected = selectedMediaIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isAllSelected = filteredMedia.combine(selectedMediaIds) { all, selected ->
        all.isNotEmpty() && all.size == selected.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    //Available playlists for PlaylistPicker
    private val _availablePlaylists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val availablePlaylists = _availablePlaylists.asStateFlow()

    val hasMedia: StateFlow<Boolean> = _allMedia
        .map { list -> list.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )


    init {
        //Get initial sort order to use from settings
        viewModelScope.launch {
            _sortOrder.value = settingsRepository.defaultMediaSortOrderFlow.first()
        }

        //Validate URI integrity and notify user if applicable
        viewModelScope.launch {
            val numStale = mediaRepository.validateMediaUris()
            if (numStale > 0) sendUiEvent(event = UiEvent.ShowToast(
                    message = "$numStale media have invalid file paths. View in Home screen via filter option.",
                    length = Toast.LENGTH_LONG
                )
            )
        }
    }

    fun refreshAvailablePlaylists(mediaIds: List<Int>) {
        viewModelScope.launch {
            _availablePlaylists.value = playlistRepository.getPlaylistsNotHavingMediaList(mediaIds)
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _selectedMediaIds.value = emptyList() //Clear selections
    }

    fun onSortOrderChange(newOrder: MediaSortOrder) {
        _sortOrder.value = newOrder
    }

    fun toggleSelection(mediaId: Int) {
        _selectedMediaIds.update { currentList ->
            if (currentList.contains(mediaId)) currentList - mediaId  //Remove from selection list
            else currentList + mediaId  //Add to selection list
        }
    }

    fun toggleSelectAll() {
        _selectedMediaIds.value =
            if (_selectedMediaIds.value.size == filteredMedia.value.size) emptyList() //Deselect all
            else filteredMedia.value //Select all
                .filter { !it.isStaleUri }
                .map { it.mediaId }
                .toList()
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptyList()
    }

    fun getCommonCreator(ids: List<Int>): String {
        return filteredMedia.value.filter { it.mediaId in ids }.getCommonCreator()
    }

    fun getCommonArtwork(ids: List<Int>): String? {
        return filteredMedia.value.filter { it.mediaId in ids }.getCommonArtwork()
    }

    fun importMedia(uriList: List<Uri>) = launchWithLoading {
        try {
            val newIds = mediaRepository.importMedia(uriList)
            sendUiEvent(UiEvent.ShowToast("Added ${newIds.size} item${if (newIds.size > 1) "s" else ""} to library" ))
            _selectedMediaIds.value += newIds
        } catch (e: Exception) {
            sendUiEvent(UiEvent.ShowToast("Failed to import media"))
        }
    }

    fun relinkMedia(mediaId: Int, newUri: Uri) = launchWithoutLoading {
        try {
            mediaRepository.relinkMedia(mediaId, newUri)
            sendUiEvent(UiEvent.ShowToast("File re-linked successfully."))
        } catch (e: Exception) {
            sendUiEvent(UiEvent.ShowToast("Failed to link new file."))
        }
    }

    fun deleteMediaByIds(ids: List<Int>) = launchWithLoading {
        mediaRepository.deleteMediaList(ids) //Perform db deletions
        _selectedMediaIds.value -= ids.toSet() //Remove from selection list since they no longer exist
        sendUiEvent(UiEvent.ShowToast("Removed ${ids.size} item${if (ids.size > 1) "s" else ""} from library"))
    }

    fun updateMediaItem(item: MediaEntity) = launchWithoutLoading {
        mediaRepository.updateMedia(item) //Perform db update
        sendUiEvent(UiEvent.ShowToast("Changes saved"))
    }

    fun updateCreatorBulk(creator: String, ids: List<Int>) = launchWithLoading {
        mediaRepository.updateCreatorBulk(creator, ids) //Perform db update
        sendUiEvent(UiEvent.ShowToast("${ids.size} item${if (ids.size > 1) "s" else ""} updated"))
    }

    fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) = launchWithLoading {
        mediaRepository.updateArtworkBulk(artworkUri, ids) //Perform db update
        sendUiEvent(UiEvent.ShowToast("${ids.size} item${if (ids.size > 1) "s" else ""} updated"))
    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) = launchWithLoading {
        playlistRepository.addMediaToPlaylists(mediaIds, playlistIds) //Perform db insert
        sendUiEvent(UiEvent.ShowToast("${mediaIds.size} item${if (mediaIds.size > 1) "s" else ""} added to ${playlistIds.size} playlist${if (playlistIds.size > 1) "s" else ""}"))
    }

    fun createPlaylist(playlist: PlaylistEntity, mediaIdsContext: List<Int> = emptyList()) = launchWithoutLoading {
        playlistRepository.insertPlaylist(playlist) //Perform db insert
        if (mediaIdsContext.isNotEmpty()) refreshAvailablePlaylists(mediaIdsContext)
        sendUiEvent(UiEvent.ShowToast("Playlist created"))
    }
}
