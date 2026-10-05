package com.devball.jubalaudio.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.data.local.entity.MediaEntity
import com.devball.jubalaudio.data.local.entity.PlaylistEntity
import com.devball.jubalaudio.data.repository.MediaRepository
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.delegates.MediaActionsDelegate
import com.devball.jubalaudio.delegates.PlaylistActionsDelegate
import com.devball.jubalaudio.delegates.SearchDelegate
import com.devball.jubalaudio.delegates.SelectionDelegate
import com.devball.jubalaudio.viewmodels.events.UiEvent
import com.devball.jubalaudio.utilgen.getCommonArtwork
import com.devball.jubalaudio.utilgen.getCommonCreator
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
    private val searchDelegate: SearchDelegate,
    private val selectionDelegate: SelectionDelegate,
    private val mediaActionsDelegate: MediaActionsDelegate,
    private val playlistActionsDelegate: PlaylistActionsDelegate
): BaseViewModel(),
    SearchDelegate by searchDelegate,
    SelectionDelegate by selectionDelegate,
    MediaActionsDelegate by mediaActionsDelegate,
    PlaylistActionsDelegate by playlistActionsDelegate {

    //Get clicked playlist id straight from navigation arguments
    private val playlistId: Int = checkNotNull(savedStateHandle["id"])

    //Get the actual playlist from db
    val playlistWithCount = playlistRepository.getPlaylistWithCountById(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    //Get all media items in this playlist
    val playlistMedia = playlistRepository.getMediaInPlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //Filtered list by combining with search query
    var filteredMedia = combine(playlistMedia, searchQuery) { media, query ->
        if (query.isBlank()) //Search field empty, show whole list
            media
        else { //Only show list where title or creator contains query (case insensitive)
            media.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.creator.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        //Bind delegates
        bindSelectionScope(viewModelScope)
        bindMediaActions(this)
        bindPlaylistActions(this)
    }

    //Override SearchDelegate's onSearchQueryChange because changing search query should also clear selection
    override fun onSearchQueryChange(newQuery: String) {
        searchDelegate.onSearchQueryChange(newQuery)
        selectionDelegate.clearSelection()
    }

    fun toggleSelectAll() {
        toggleSelectAll(filteredMedia.value
            .filter { !it.isStaleUri }
            .map { it.mediaId }
        )
    }

    fun updatePlaylistOrder(sortedMediaIds: List<Int>) = launchWithoutLoading {
        playlistRepository.updatePlaylistOrder(playlistId, sortedMediaIds)
        showToast("Playlist Order Saved")
    }

    suspend fun getMediaNotInPlaylist(): List<MediaEntity> {
        return playlistRepository.getMediaNotInPlaylist(playlistId)
    }
}
