package com.devball.jubalaudio.viewmodels

import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.data.local.entity.MediaEntity
import com.devball.jubalaudio.data.local.entity.PlaylistEntity
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.data.repository.UserPreferencesRepository
import com.devball.jubalaudio.utilgen.PlaylistsSortOrder
import com.devball.jubalaudio.viewmodels.events.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : BaseViewModel() {

    //For searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //Sort State
    val sortOrder: StateFlow<PlaylistsSortOrder> = userPreferencesRepository.playlistsSortOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.INITIAL_PLAYLISTS_SORT_ORDER)

    //Get all playlist entities from the db
    private val _allPlaylists = playlistRepository.allPlaylistsWithCounts

    //Filter full list by combining with search query
    val filteredPlaylists = combine(_allPlaylists, _searchQuery, sortOrder) { playlists, query, sort ->
        //Filter first
        val filtered = if (query.isBlank()) //Search field empty, show whole list
            playlists
        else { //Only show list where name or description (if exists) contains query (case insensitive)
            playlists.filter { item ->
                item.playlist.name.contains(query, ignoreCase = true) ||
                (item.playlist.description?.contains(query, ignoreCase = true) ?: false)
            }
        }
        //Then sort
        when (sort) {
            PlaylistsSortOrder.NAME_ASC -> filtered.sortedBy { it.playlist.name.lowercase() }
            PlaylistsSortOrder.NAME_DESC -> filtered.sortedByDescending { it.playlist.name.lowercase() }
            PlaylistsSortOrder.DATE_CREATED_MOST_RECENT -> filtered.sortedByDescending { it.playlist.dateCreated }
            PlaylistsSortOrder.DATE_CREATED_LEAST_RECENT -> filtered.sortedBy { it.playlist.dateCreated }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    init {

    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSortOrderChange(newOrder: PlaylistsSortOrder) = launchWithoutLoading {
        userPreferencesRepository.updatePlaylistsSortOrder(newOrder)
    }

    fun createPlaylist(playlist: PlaylistEntity) = launchWithoutLoading {
        playlistRepository.insertPlaylist(playlist) //Perform db insert
        sendUiEvent(UiEvent.ShowToast("Playlist created"))
    }

    fun editPlaylist(playlist: PlaylistEntity) = launchWithoutLoading {
        playlistRepository.updatePlaylist(playlist) //Perform db insert
        sendUiEvent(UiEvent.ShowToast("Playlist details saved"))
    }

    fun deletePlaylist(playlist: PlaylistEntity) = launchWithLoading {
        playlistRepository.deletePlaylist(playlist)
        sendUiEvent(UiEvent.ShowToast("Playlist deleted"))
    }

    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) = launchWithLoading {
        playlistRepository.addMediaToPlaylists(mediaIds, playlistIds)
        sendUiEvent(UiEvent.ShowToast("${mediaIds.size} item${if (mediaIds.size > 1) "s" else ""} added to ${playlistIds.size} playlist${if (playlistIds.size > 1) "s" else ""}"))
    }

    suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity> {
        return playlistRepository.getMediaNotInPlaylist(playlistId)
    }
}
