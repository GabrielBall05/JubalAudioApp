package com.devball.jubalaudio.delegates

import com.devball.jubalaudio.data.local.entity.PlaylistEntity
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.viewmodels.UiActionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface PlaylistActionsDelegate {
    val availablePlaylists: StateFlow<List<PlaylistEntity>>
    fun refreshAvailablePlaylists(mediaIds: List<Int>)
    fun createPlaylist(playlist: PlaylistEntity, mediaIdsContext: List<Int> = emptyList())
    fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>)
    fun editPlaylist(playlist: PlaylistEntity)
    fun deletePlaylist(playlist: PlaylistEntity)
    fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int)
    fun bindPlaylistActions(handler: UiActionHandler)
}

class PlaylistActionsDelegateImpl @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : PlaylistActionsDelegate {
    private var uiHandler: UiActionHandler? = null

    private val _availablePlaylists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    override val availablePlaylists = _availablePlaylists.asStateFlow()

    override fun bindPlaylistActions(handler: UiActionHandler) { this.uiHandler = handler }
    private fun handler() = uiHandler ?: throw IllegalStateException("Delegate not bound")

    override fun refreshAvailablePlaylists(mediaIds: List<Int>) = handler().launchWithoutLoading {
        _availablePlaylists.value = playlistRepository.getPlaylistsNotHavingMediaList(mediaIds)
    }

    override fun createPlaylist(playlist: PlaylistEntity, mediaIdsContext: List<Int>) = handler().launchWithoutLoading {
        playlistRepository.insertPlaylist(playlist)
        if (mediaIdsContext.isNotEmpty()) refreshAvailablePlaylists(mediaIdsContext)
        handler().showToast("Playlist created")
    }

    override fun addMediaToPlaylists(mediaIds: List<Int>, playlistIds: List<Int>) = handler().launchWithLoading {
        playlistRepository.addMediaToPlaylists(mediaIds, playlistIds)
        handler().showToast("${mediaIds.size} item${if (mediaIds.size > 1) "s" else ""} added to ${playlistIds.size} playlist${if (playlistIds.size > 1) "s" else ""}")
    }

    override fun editPlaylist(playlist: PlaylistEntity) = handler().launchWithoutLoading {
        playlistRepository.updatePlaylist(playlist)
        handler().showToast("Playlist details saved")
    }

    override fun deletePlaylist(playlist: PlaylistEntity) = handler().launchWithLoading {
        playlistRepository.deletePlaylist(playlist)
        handler().showToast("Playlist deleted")
    }

    override fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int) = handler().launchWithLoading {
        playlistRepository.removeMediaFromPlaylist(mediaIds, playlistId)
        handler().showToast("${mediaIds.size} item${if (mediaIds.size > 1) "s" else ""} removed from playlist")
    }
}