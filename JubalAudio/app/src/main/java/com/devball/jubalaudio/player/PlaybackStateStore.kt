package com.devball.jubalaudio.player

import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateStore @Inject constructor() {
    private val _currentPlaylistId = MutableStateFlow<Int?>(null)
    val currentPlaylistId = _currentPlaylistId.asStateFlow()

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling = _isShuffling.asStateFlow()

    var originalPlaylist: List<MediaItem> = emptyList()
        set(value) { field = value.distinctBy { it.mediaId } }

    fun setPlaylistId(id: Int?) { _currentPlaylistId.value = id }
    fun toggleShuffle() { _isShuffling.value = !_isShuffling.value }
    fun setShuffling(shuffling: Boolean) { _isShuffling.value = shuffling }

    fun preparePlaylistTimeline(
        mediaItems: List<MediaItem>,
        playlistId: Int?,
        startItemIndex: Int,
        startShuffled: Boolean
    ): Pair<List<MediaItem>, Int> {
        //Ensure no duplicates
        val items = mediaItems.distinctBy { it.mediaId }

        //Store clean list
        originalPlaylist = items

        //Apply states
        _currentPlaylistId.value = playlistId
        _isShuffling.value = startShuffled

        //Verify chosen start index is within bounds
        val startAtSpecific = startItemIndex >= 0 && startItemIndex < items.size
        val finalTimeline: List<MediaItem>
        val playIndex: Int

        //If shuffle is ON:
        if (startShuffled) {
            //Set first item to desired start item if applicable, otherwise choose random item
            val startIndex = if (startAtSpecific) startItemIndex else items.indices.random()
            val startingItem = items[startIndex]
            //Shuffle remaining items
            val remaining = items.filterIndexed { index, _ -> index != startIndex }.shuffled()
            //Set timeline to newly shuffled list
            finalTimeline = listOf(startingItem) + remaining
            //Start at beginning
            playIndex = 0
        }
        //If shuffle is OFF:
        else {
            //Set timeline to full, ordered playlist
            finalTimeline = items
            //Start at desired start item if applicable, otherwise start at beginning
            playIndex = if (startAtSpecific) startItemIndex else 0
        }

        return Pair(finalTimeline, playIndex)
    }

    fun clear() {
        _currentPlaylistId.value = null
        _isShuffling.value = false
        originalPlaylist = emptyList()
    }
}