package com.devball.jubalaudio.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.data.local.PlaylistEntity
import com.devball.jubalaudio.data.repository.MediaRepository
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.util.UiEvent
import com.devball.jubalaudio.util.getCommonArtwork
import com.devball.jubalaudio.util.getCommonCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel() {

    //Get clicked playlist id straight from navigation arguments
    private val playlistId: Int = checkNotNull(savedStateHandle["id"])

    //Get the actual playlist from db
    val playlistWithCount = playlistRepository.getPlaylistWithCountById(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    //Get all media items in this playlist
    val playlistMedia = playlistRepository.getMediaInPlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    var filteredMedia = combine(playlistMedia, _searchQuery) { media, query ->
        if (query.isBlank()) //Search field empty, show whole list
            media
        else { //Only show list where title or creator contains query (case insensitive)
            media.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.creator.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun refreshAvailablePlaylists(mediaIds: List<Int>) {
        viewModelScope.launch {
            _availablePlaylists.value = playlistRepository.getPlaylistsNotHavingMediaList(mediaIds)
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _selectedMediaIds.value = emptyList()
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

    fun relinkMedia(mediaId: Int, newUri: Uri) = launchWithoutLoading {
        try {
            mediaRepository.relinkMedia(mediaId, newUri)
            sendUiEvent(UiEvent.ShowToast("File re-linked successfully."))
        } catch (e: Exception) {
            sendUiEvent(UiEvent.ShowToast("Failed to link new file."))
        }
    }

    fun editPlaylist(playlist: PlaylistEntity) = launchWithoutLoading {
        playlistRepository.updatePlaylist(playlist)
        sendUiEvent(UiEvent.ShowToast("Playlist details saved"))
    }

    fun deletePlaylist(playlist: PlaylistEntity) = launchWithLoading {
        playlistRepository.deletePlaylist(playlist)
        sendUiEvent(UiEvent.ShowToast("Playlist deleted"))
    }

    fun removeMediaFromPlaylist(ids: List<Int>) = launchWithLoading {
        playlistRepository.removeMediaFromPlaylist(ids, playlistId)
        sendUiEvent(UiEvent.ShowToast("${ids.size} item${if (ids.size > 1) "s" else ""} removed from playlist"))
    }

    fun updateMediaItem(item: MediaEntity) = launchWithoutLoading {
        mediaRepository.updateMedia(item) //Perform db update
        sendUiEvent(UiEvent.ShowToast("Changes saved"))
    }

    fun moveMediaItemPosition(fromMediaId: Int, toMediaId: Int) {
        //Get indices from full media list
        val fromPos = playlistMedia.value.indexOfFirst { it.mediaId == fromMediaId }
        val toPos = playlistMedia.value.indexOfFirst { it.mediaId == toMediaId }
        if (fromPos == -1 || toPos == -1) return

        //Perform db update
        launchWithoutLoading {
            playlistRepository.moveMediaItemPositionInPlaylist(playlistId, fromMediaId, toMediaId, fromPos, toPos)
        }
    }

    fun updateCreatorBulk(creator: String, ids: List<Int>) = launchWithLoading {
        mediaRepository.updateCreatorBulk(creator, ids)
        sendUiEvent(UiEvent.ShowToast("${ids.size} item${if (ids.size > 1) "s" else ""} updated"))

    }

    fun updateArtworkBulk(artworkUri: String?, ids: List<Int>) = launchWithLoading {
        mediaRepository.updateArtworkBulk(artworkUri, ids) //Perform db update
        sendUiEvent(UiEvent.ShowToast("${ids.size} item${if (ids.size > 1) "s" else ""} updated"))

    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) = launchWithLoading {
        playlistRepository.addMediaToPlaylists(mediaIds, playlistIds)
        sendUiEvent(UiEvent.ShowToast("${mediaIds.size} item${if (mediaIds.size > 1) "s" else ""} added to ${playlistIds.size} playlist${if (playlistIds.size > 1) "s" else ""}"))
    }

    fun createPlaylist(playlist: PlaylistEntity, mediaIdsContext: List<Int> = emptyList()) = launchWithoutLoading {
        playlistRepository.insertPlaylist(playlist) //Perform db insert
        if (mediaIdsContext.isNotEmpty()) refreshAvailablePlaylists(mediaIdsContext)
        sendUiEvent(UiEvent.ShowToast("Playlist created"))
    }

    suspend fun getMediaNotInPlaylist(): List<MediaEntity> {
        return playlistRepository.getMediaNotInPlaylist(playlistId)
    }
}
