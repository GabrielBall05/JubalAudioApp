package com.devball.jubalaudio.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devball.jubalaudio.data.local.asManualQueueItem
import com.devball.jubalaudio.data.local.toMediaItem
import com.devball.jubalaudio.data.repository.PlaybackPersistenceRepository
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.data.repository.SettingsRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerManager @Inject constructor(
    private val persistenceRepository: PlaybackPersistenceRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) {
    //Controller setup / instance: Manages asynchronous connection to ExoPlayer and holds active reference
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    //Playback metadata / states for UI
    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    //States for playback modes for UI
    private val _isShuffling = MutableStateFlow(false)
    val isShuffling = _isShuffling.asStateFlow()

    private val _repeatingCurrent = MutableStateFlow(false)
    val repeatingCurrent = _repeatingCurrent.asStateFlow()

    //Queue States for UI - sourced from actual ExoPlayer timeline
    private val _manualQueueState = MutableStateFlow<List<MediaItem>>(emptyList())
    val manualQueueState = _manualQueueState.asStateFlow()

    private val _upNextState = MutableStateFlow<List<MediaItem>>(emptyList())
    val upNextState = _upNextState.asStateFlow()

    //Current playlist state for UI if applicable
    private val _currentPlaylistId = MutableStateFlow<Int?>(null)
    val currentPlaylistId = _currentPlaylistId.asStateFlow()

    //Copy of the original playlist populated the moment a playlist starts playing
    private var originalPlaylist: List<MediaItem> = emptyList()

    //State for broadcasting error messages that may occur here
    private val _errorMessage = Channel<String>(Channel.BUFFERED)
    val errorMessage = _errorMessage.receiveAsFlow()


    init {
        setupController() //Set up controller connection and add listeners
    }

    fun updateCurrentPosition() { //For UI to display duration
        val player = controller ?: return
        _currentPosition.value = player.currentPosition
        _duration.value = player.duration.coerceAtLeast(0L)
    }

    //Seek to next media item
    fun seekToNext() {
        val player = controller ?: return
        player.seekToNext()
        player.play()
    }

    //Seek to previous media item
    fun seekToPrevious() {
        val player = controller ?: return
        player.seekToPrevious()
        player.play()
    }

    //Seeking within current media item
    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        player.seekTo(positionMs)
    }

    //Toggle playing state
    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause()else player.play()
    }

    //Toggle repeating current
    fun toggleRepeatMode() {
        val player = controller ?: return
        _repeatingCurrent.value = !_repeatingCurrent.value //Toggle state
        //Apply new repeat mode: ON = repeat current media item indefinitely; OFF = standard linear playback
        player.repeatMode = if (_repeatingCurrent.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    //Toggle shuffle mode
    fun toggleShuffle() {
        val player = controller ?: return
        if (originalPlaylist.isEmpty() || player.mediaItemCount == 0) return

        _isShuffling.value = !_isShuffling.value //Toggle state

        //Get current index, current size of manual queue, and current start index for up next (after manual queue)
        val currentIndex = player.currentMediaItemIndex
        val manualQueueSize = _manualQueueState.value.size
        val upNextStartIndex = currentIndex + 1 + manualQueueSize

        if (upNextStartIndex >= player.mediaItemCount) return // Nothing to shuffle

        //Extract only up next items (don't touch manual queue)
        val currentUpNext = mutableListOf<MediaItem>()
        for (i in upNextStartIndex until player.mediaItemCount) {
            currentUpNext.add(player.getMediaItemAt(i))
        }

        //Turning shuffle ON: New up next list becomes remaining up next list shuffled
        val newUpNext = if (_isShuffling.value) currentUpNext.shuffled()
        //Turning shuffle OFF: New up next list becomes natural order based on originalPlaylist
        else {
            //Sort current up next by its index in originalPlaylist
            currentUpNext.sortedBy { item ->
                val realId = item.mediaMetadata.extras?.getString("ORIGINAL_MEDIA_ID") ?: item.mediaId
                val index = originalPlaylist.indexOfFirst { it.mediaId == realId }
                if (index != -1) index else Int.MAX_VALUE
            }
        }

        //Replace upcoming items without touching the current item or manual queue items - uninterrupted playback
        player.replaceMediaItems(upNextStartIndex, player.mediaItemCount, newUpNext)
    }

    //Play and skip-to
    fun playNow(mediaItem: MediaItem) {
        val player = controller ?: return

        //Make it a manual queue item
        val taggedItem = mediaItem.asManualQueueItem()

        //Add to front if no items present, otherwise add immediately after current item
        val insertIndex = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        player.addMediaItem(insertIndex, taggedItem)
        player.seekTo(insertIndex, 0L) //Seek to new item
        player.play() //Start playing new item
    }

    //Clear timeline and play playlist
    fun playPlaylist(mediaItems: List<MediaItem>, playlistId: Int?, startItemIndex: Int, startShuffled: Boolean) {
        if (mediaItems.isEmpty()) return
        val player = controller ?: return

        //TODO: Maybe preserve manual queue where applicable

        //Make copy of full playlist
        originalPlaylist = mediaItems
        //Apply states
        _currentPlaylistId.value = playlistId
        _isShuffling.value = startShuffled

        val startAtSpecific = startItemIndex >= 0
        val finalTimeline: List<MediaItem>
        val playIndex: Int

        //If shuffle is ON:
        if (startShuffled) {
            //Set first item to desired start item if applicable, otherwise choose random item
            val startIndex = if (startAtSpecific) startItemIndex else mediaItems.indices.random()
            val startingItem = mediaItems[startIndex]
            //Shuffle remaining items
            val remaining = mediaItems.filterIndexed { index, _ -> index != startIndex }.shuffled()

            //Set timeline to newly shuffled list
            finalTimeline = listOf(startingItem) + remaining
            //Start at beginning
            playIndex = 0
        }
        //If shuffle is OFF:
        else {
            //Set timeline to full, ordered playlist
            finalTimeline = mediaItems
            //Start at desired start item if applicable, otherwise start at beginning
            playIndex = if (startAtSpecific) startItemIndex else 0
        }

        //Apply timeline to ExoPlayer
        player.setMediaItems(finalTimeline)
        //Seek to specified start item
        player.seekTo(playIndex, 0L)
        player.prepare()
        player.play() //Start playing

        saveCurrentStateToDisk() //Save new information
    }

    //Add items to manual queue
    fun addToQueue(mediaItems: List<MediaItem>) {
        val player = controller ?: return
        if (mediaItems.isEmpty()) return

        //Make all items manual queue items
        val taggedItems = mediaItems.map { it.asManualQueueItem() }

        //Insert right after last existing manual queue item (FIFO)
        val insertIndex = player.currentMediaItemIndex + 1 + _manualQueueState.value.size
        player.addMediaItems(insertIndex, taggedItems) //Add to timeline
    }

    //Clear manual queue
    fun clearQueue() {
        val player = controller ?: return
        val currentIndex = player.currentMediaItemIndex

        //Iterate backward through the manual queue items to safely remove them
        for (i in player.mediaItemCount - 1 downTo currentIndex + 1) {
            val item = player.getMediaItemAt(i)
            //Remove if tagged as manual queue item
            if (item.mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true) {
                player.removeMediaItem(i) //Apply removal from timeline
            }
        }
    }

    //Move manual queue item in timeline
    fun moveManualQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex == toIndex || fromIndex !in _manualQueueState.value.indices || toIndex !in _manualQueueState.value.indices) return

        //+1 because manual queue starts immediately after current item
        val actualFrom = player.currentMediaItemIndex + 1 + fromIndex
        val actualTo = player.currentMediaItemIndex + 1 + toIndex

        //Perform move
        player.moveMediaItem(actualFrom, actualTo)
    }

    //Move up next item in timeline
    fun moveUpNextItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex == toIndex || fromIndex !in _upNextState.value.indices || toIndex !in _upNextState.value.indices) return

        //+1 because manual queue starts immediately after current item
        //+manual queue size because up next starts immediately after manual queue
        val offset = player.currentMediaItemIndex + 1 + _manualQueueState.value.size
        val actualFrom = offset + fromIndex
        val actualTo = offset + toIndex

        //Perform move
        player.moveMediaItem(actualFrom, actualTo)
    }

    //Skip to manual queue item
    fun manualQueueSkipToIndex(index: Int) {
        val player = controller ?: return
        if (index !in _manualQueueState.value.indices) return

        //Target becomes index of manual queue + 1
        val targetIndex = player.currentMediaItemIndex + 1 + index
        player.seekTo(targetIndex, 0L) //Perform skip
        player.play() //onMediaItemTransition handles consuming the skipped items
    }

    //Skip to up next item
    fun upNextSkipToIndex(index: Int) {
        val player = controller ?: return
        if (index !in _upNextState.value.indices) return

        val currentIndex = player.currentMediaItemIndex
        val manualQueueSize = _manualQueueState.value.size

        //If manual queue isn't empty, move entire manual queue to after target item, then perform skip
        if (manualQueueSize > 0) {
            val queueItems = mutableListOf<MediaItem>()
            //Get start and end indices of manual queue
            val queueStartIndex = currentIndex + 1
            val queueEndIndex = queueStartIndex + manualQueueSize

            //Extract manual queue items
            for (i in queueStartIndex until queueEndIndex) {
                queueItems.add(player.getMediaItemAt(i))
            }

            //Erase them from their old position (removeMediaItems is exclusive of the end index)
            player.removeMediaItems(queueStartIndex, queueEndIndex)

            //Calculate true target index (manual queue is gone from timeline)
            //+1 because up next is guaranteed to be immediately after current item
            val newTargetIndex = currentIndex + 1 + index

            //Inject extracted manual queue immediately after the target item
            player.addMediaItems(newTargetIndex + 1, queueItems)

            //Perform skip
            player.seekTo(newTargetIndex, 0L)
        }
        //If manual queue is empty, just calculate target index and perform skip
        else {
            //+1 because up next is immediately after current item
            val targetIndex = currentIndex + 1 + index
            player.seekTo(targetIndex, 0L) //Perform skip
        }

        player.play() //Ensure player is playing
    }

    //Remove item
    fun removeItemAtIndex(index: Int, isManual: Boolean) {
        val player = controller ?: return
        if (isManual && index !in _manualQueueState.value.indices) return
        if (!isManual && index !in _upNextState.value.indices) return

        //Calculate actual index which is index + 1 away from current item
        var actualIndex = player.currentMediaItemIndex + 1 + index
        //If item is an up next item, actual index is manual queue size further away from current
        if (!isManual) actualIndex += _manualQueueState.value.size

        player.removeMediaItem(actualIndex) //Perform removal
    }

    //Set up controller connection and add listeners
    fun setupController() {
        if (controllerFuture != null) return

        //Create SessionToken for the PlaybackService and asynchronously build the MediaController to interact with it
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        //Attempt to add listeners
        controllerFuture?.addListener({
            try {
                val player = controllerFuture?.get() ?: return@addListener
                controller = player

                //Set UI states to actual player states
                _currentMediaItem.value = player.currentMediaItem
                _isPlaying.value = player.isPlaying
                _duration.value = player.duration.coerceAtLeast(0L)
                _currentPosition.value = player.currentPosition
                _repeatingCurrent.value = player.repeatMode == Player.REPEAT_MODE_ONE

                player.addListener(object : Player.Listener {
                    //Executes when a media item transition occurs
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)

                        //Update current item and duration states
                        _currentMediaItem.value = mediaItem
                        _duration.value = player.duration.coerceAtLeast(0L)

                        //Remove all past manual queue items
                        consumePastQueueItems(player)
                        //Update all UI states to sync with ExoPlayer's timeline
                        updateUIStates(player)
                    }

                    //Executes when ExoPlayer's timeline changes
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        super.onTimelineChanged(timeline, reason)
                        //Update all UI states to sync with ExoPlayer's timeline
                        updateUIStates(player)

                        //Whenever timeline changes, save it
                        saveCurrentStateToDisk()
                    }

                    //Executes whenever playing state changes
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        _isPlaying.value = isPlaying //Update UI state
                    }

                    //Executes whenever playback state changes
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)

                        //Update duration if player is able to play
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = player.duration.coerceAtLeast(0L)
                        }

                        //If end of timeline has been reached and infinite playback setting is on, repeat original playlist
                        if (playbackState == Player.STATE_ENDED) _currentPlaylistId.value?.let { playlistId ->
                            CoroutineScope(Dispatchers.IO).launch { //Use IO thread
                                //Only continue if infinite playlist setting is on
                                if (settingsRepository.infinitePlaybackFlow.first()) {
                                    //Fetch updated playlist list from Room database
                                    val freshItems = playlistRepository.fetchPlaylistMediaList(playlistId).map { it.toMediaItem() }
                                    if (freshItems.isNotEmpty()) {
                                        //Switch back to Main thread for MediaControllerManager operations
                                        withContext(Dispatchers.Main) {
                                            //Reuse playPlaylist and use current values
                                            playPlaylist(
                                                mediaItems = freshItems,
                                                playlistId = playlistId,
                                                startItemIndex = 0,
                                                startShuffled = _isShuffling.value
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //Executes when the player's position changes (ex: transitioning, seeking, or discontinuity occurs)
                    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                        saveCurrentStateToDisk() //Save whenever a new item is jumped to
                    }

                    //Executes when a player error occurs
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        val player = controller ?: return

                        //If error is related to loading the resource (ex: Stale URI)
                        if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                            || error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION
                            || error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                        ) {
                            //Get current item, its title (name), and actual mediaId
                            val currentItem = player.currentMediaItem
                            val failedItemTitle: String? = player.currentMediaItem?.mediaMetadata?.title?.toString()
                            val failedMediaId = currentItem?.mediaMetadata?.extras?.getString("ORIGINAL_MEDIA_ID")
                                ?: currentItem?.mediaId

                            //Send error message (UI event)
                            CoroutineScope(Dispatchers.Main).launch {
                                _errorMessage.send(element =
                                    if (failedItemTitle != null) "File not found or unavailable for '$failedItemTitle.'"
                                    else "File not found or unavailable."
                                )
                            }

                            //Remove broken item from originalPlaylist list so it doesn't end up in timeline again
                            originalPlaylist = originalPlaylist.filter { it.mediaId != failedMediaId }

                            //Remove all instances of this broken item from the timeline
                            for (i in player.mediaItemCount - 1 downTo 0) {
                                val item = player.getMediaItemAt(i)
                                val itemId = item.mediaMetadata.extras?.getString("ORIGINAL_MEDIA_ID") ?: item.mediaId
                                if (itemId == failedMediaId) player.removeMediaItem(i) //Perform removal
                            }

                            //Play the next item (next item automatically becomes the new current after removing the broken item)
                            if (player.mediaItemCount > 0) {
                                player.prepare()
                                player.play()
                            }
                        }
                    }
                })

                //Restoration - after adding listeners
                if (player.mediaItemCount == 0) restorePlaybackState() //If player is empty (cold start), restore playback from disk
                else updateUIStates(player) //Otherwise (app relaunched while music playing), just let the ui sync to the live player
            }
            //Setup failed:
            catch (e: Exception) {
                Log.e("OfflineAudioSuite", "MCM: Failed to connect", e)
                controllerFuture = null
            }
        }, MoreExecutors.directExecutor())
    }


    //Rebuild UI lists dynamically based on ExoPlayer's timeline.
    private fun updateUIStates(player: Player) {
        val currentIndex = player.currentMediaItemIndex

        //Reset states and return if the player has no media or the current index is invalid
        if (currentIndex == C.INDEX_UNSET || player.mediaItemCount == 0) {
            _manualQueueState.value = emptyList()
            _upNextState.value = emptyList()
            return
        }

        val manualQueue = mutableListOf<MediaItem>()
        val upNext = mutableListOf<MediaItem>()

        //Build manual queue and up next lists based on timeline immediately after current item
        for (i in currentIndex + 1 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            //If item is tagged as manual queue item, add it to manual queue temp list
            if (item.mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true) manualQueue.add(item)
            else upNext.add(item) //Otherwise add it to up next temp list
        }

        //Update actual states with temp lists
        _manualQueueState.value = manualQueue
        _upNextState.value = upNext
    }

    //Erase all manual queue items from timeline BEFORE current item
    private fun consumePastQueueItems(player: Player) {
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET) return

        //Iterate backwards from right behind the current item down to the beginning of the timeline
        for (i in currentIndex - 1 downTo 0) {
            val item = player.getMediaItemAt(i)
            //Remove item from timeline if it is a manual queue item (consume it)
            if (item.mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true) {
                player.removeMediaItem(i) //Perform removal
            }
        }
    }

    //Rebuild timeline with saved playback state (upon app launch)
    fun restorePlaybackState() {
        val player = controller ?: return

        //Use Main thread to retrieve saved data
        CoroutineScope(Dispatchers.Main).launch {
            //Get entire saved timeline
            val timelineItems = persistenceRepository.getRestoredMediaItems()
            //Get entire saved original playlist
            val originalItems = persistenceRepository.getRestoredOriginalPlaylist()
            //Get saved player data states
            val meta = persistenceRepository.playbackMetadata.first()

            if (timelineItems.isNotEmpty()) {
                //Update lists and states
                originalPlaylist = originalItems
                _currentPlaylistId.value = meta.playlistId
                _isShuffling.value = meta.isShuffling
                _repeatingCurrent.value = meta.repeatingCurrent
                _currentPosition.value = meta.position

                //Rebuild ExoPlayer's timeline
                player.setMediaItems(timelineItems)
                //Apply saved repeat mode
                player.repeatMode = if (meta.repeatingCurrent) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

                //Seek to saved index and duration (pick up where user left off)
                val targetIndex = if (meta.index >= 0 && meta.index < timelineItems.size) meta.index else 0
                player.seekTo(targetIndex, meta.position) //Perform seek
                player.prepare()

                //Immediately force UI updates
                updateUIStates(player)
            }
        }
    }

    //Save all current player data (timeline, states, lists) to disk
    private fun saveCurrentStateToDisk() {
        val player = controller ?: return

        //Capture snapshot on Main thread
        val position = player.currentPosition
        val index = player.currentMediaItemIndex
        val isShuffling = _isShuffling.value
        val isRepeating = _repeatingCurrent.value
        val playlistId = _currentPlaylistId.value
        val originalCopy = originalPlaylist.toList()

        //Extract timeline items
        val timelineItems = mutableListOf<MediaItem>()
        for (i in 0 until player.mediaItemCount) {
            timelineItems.add(player.getMediaItemAt(i))
        }

        //Pass snapshot to IO thread
        CoroutineScope(Dispatchers.IO).launch {
            persistenceRepository.savePlaybackState(
                position = position,
                index = index,
                isShuffling = isShuffling,
                isRepeating = isRepeating,
                playlistId = playlistId,
                timelineItems = timelineItems,
                originalItems = originalCopy
            )
        }
    }

    //Save just the position (index and duration) to disk
    fun savePositionOnly() {
        val player = controller ?: return
        if (!player.isPlaying) return

        //Capture snapshot on main thread
        val index = player.currentMediaItemIndex
        val position = player.currentPosition

        //Perform save on IO thread
        CoroutineScope(Dispatchers.IO).launch {
            persistenceRepository.savePlaybackPosition(index, position)
        }
    }

    //Completely wipe out player timeline and reset states
    fun nukePlayer() {
        val player = controller ?: return

        //Stop player and clear timeline
        player.stop()
        player.clearMediaItems()

        //Reset internal states so UI reflects the empty nature
        _currentMediaItem.value = null
        _currentPosition.value = 0L
        _duration.value = 0L
        _isPlaying.value = false
        _isShuffling.value = false //TODO: Maybe keep shuffle setting
        _repeatingCurrent.value = false
        _manualQueueState.value = emptyList()
        _upNextState.value = emptyList()
        originalPlaylist = emptyList()
        _currentPlaylistId.value = null

        //Clear the disk's saved state so it doesn't get rebuilt on app launch
        CoroutineScope(Dispatchers.IO).launch { //Perform on IO thread
            persistenceRepository.clearPlaybackState()
        }
    }

    //Release connection to the PlaybackService
    fun releaseController() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
            controllerFuture = null
            controller = null
        }
    }
}