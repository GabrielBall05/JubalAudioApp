package com.devball.jubalaudio.player

import androidx.annotation.OptIn
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.devball.jubalaudio.data.local.toMediaItem
import com.devball.jubalaudio.data.repository.PlaybackPersistenceRepository
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService() : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer

    @Inject lateinit var persistenceRepository: PlaybackPersistenceRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var playbackStateStore: PlaybackStateStore

    //Coroutine scope dedicated to background persistence and rules
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    //Coroutine scope dedicated to updating PlayerWidget
    private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    //For background position saving
    private var positionSaveJob: Job? = null

    private val playerListener = object : Player.Listener {

        //Executes when a media item transition occurs
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            //Remove all past manual queue items
            consumePastManualQueueItems(exoPlayer)
        }

        //Executes when ExoPlayer's timeline changes
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            //Whenever timeline changes, save it
            saveCurrentStateToDisk(exoPlayer)
        }

        //Executes when the player's position changes (ex: transitioning, seeking, or discontinuity occurs)
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            //Save whenever a new item is jumped to
            saveCurrentStateToDisk(exoPlayer)
        }

        //Executes whenever playing state changes
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            //If playing, start background saving ticker
            if (isPlaying) {
                positionSaveJob?.cancel()
                positionSaveJob = serviceScope.launch {
                    while (isActive) {
                        delay(3000L) //Save every 3 seconds
                        savePositionOnly(exoPlayer)
                    }
                }
            }
            //If not playing, stop background ticker and perform one final save
            else {
                positionSaveJob?.cancel()
                saveCurrentStateToDisk(exoPlayer)
            }
        }

        //Executes whenever playback state changes
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            //If end of timeline has been reached and infinite playback setting is on, repeat original playlist
            if (playbackState == Player.STATE_ENDED) playbackStateStore.currentPlaylistId.value?.let { playlistId ->
                serviceScope.launch(Dispatchers.IO) { //Use IO thread
                    //Only continue if infinite playlist setting is on
                    if (settingsRepository.infinitePlaybackFlow.first()) {
                        //Fetch updated playlist list from Room database
                        val freshItems = playlistRepository.fetchPlaylistMediaList(playlistId).map { it.toMediaItem() }
                        if (freshItems.isNotEmpty()) {
                            //Switch back to Main thread for MediaControllerManager operations
                            withContext(Dispatchers.Main) {
                                restartPlaylistNatively(
                                    mediaItems = freshItems,
                                    playlistId = playlistId,
                                    startShuffled = playbackStateStore.isShuffling.value
                                )
                            }
                        }
                    }
                }
            }
        }

        //Executes when a player error occurs
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            //Natively handle player errors related to loading the resource (ex: Stale URI)
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                || error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION
                || error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
            ) {
                //Get current item and actual mediaId
                val currentItem = exoPlayer.currentMediaItem
                val failedMediaId = currentItem?.mediaMetadata?.extras?.getString("ORIGINAL_MEDIA_ID")
                    ?: currentItem?.mediaId

                //Remove broken item from originalPlaylist list so it doesn't end up in timeline again
                playbackStateStore.originalPlaylist = playbackStateStore.originalPlaylist.filter { it.mediaId != failedMediaId }

                //Remove all instances of this broken item from the timeline
                for (i in exoPlayer.mediaItemCount - 1 downTo 0) {
                    val item = exoPlayer.getMediaItemAt(i)
                    val itemId = item.mediaMetadata.extras?.getString("ORIGINAL_MEDIA_ID") ?: item.mediaId
                    if (itemId == failedMediaId) exoPlayer.removeMediaItem(i) //Perform removal
                }

                //Play the next item (next item automatically becomes the new current after removing the broken item)
                if (exoPlayer.mediaItemCount > 0) {
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)

            //Trigger widget updates whenever playback state, timeline, or current item changes
            if (events.containsAny(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED
            )) { updateWidget(player) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        restoreStateOnStart()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        //Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true) //Pauses when headphones are unplugged
            .build()

        //Add detailed logging
        exoPlayer.addAnalyticsListener(EventLogger())

        //Attach listener
        exoPlayer.addListener(playerListener)

        //Wrap player in a ForwardingPlayer to intercept commands universally
        val forwardingPlayer = object : ForwardingPlayer(exoPlayer) {

            //Intercept standard Next button clicks
            override fun seekToNext() {
                //Turn off repeatingCurrent for the new current media item
                if (repeatMode == REPEAT_MODE_ONE) repeatMode = REPEAT_MODE_OFF
                super.seekToNext()
                play() //Ensure it plays
            }

            //Intercept absolute commands to seek to next media item
            override fun seekToNextMediaItem() {
                //Turn off repeatingCurrent for the new current media item
                if (repeatMode == REPEAT_MODE_ONE) repeatMode = REPEAT_MODE_OFF
                super.seekToNextMediaItem()
                play() //Ensure it plays
            }

            //Intercept standard Previous button clicks
            override fun seekToPrevious() {
                //Only execute custom seekToPrevious logic if player is actually seeking to previous
                if ((currentPosition <= maxSeekToPreviousPosition) && hasPreviousMediaItem()) {
                    //Turn off repeatingCurrent for the new current media item
                    if (repeatMode == REPEAT_MODE_ONE) repeatMode = REPEAT_MODE_OFF
                    shiftQueueAndSeek { super.seekToPrevious() } //Perform custom logic
                } else {
                    super.seekToPrevious() //Just restart the track
                }
                play()
            }

            //Intercept absolute commands to seek to previous media item
            override fun seekToPreviousMediaItem() {
                //Only execute custom logic if player is actually seeking to previous media item
                if (hasPreviousMediaItem()) {
                    //Turn off repeatingCurrent for the new current media item
                    if (repeatMode == REPEAT_MODE_ONE) repeatMode = REPEAT_MODE_OFF
                    shiftQueueAndSeek { super.seekToPreviousMediaItem() } //Perform custom logic
                } else {
                    super.seekToPreviousMediaItem()
                }
                play()
            }

            //Custom seek to previous logic
            private fun shiftQueueAndSeek(seekAction: () -> Unit) {
                val oldCurrentIndex = currentMediaItemIndex
                if ( //Use standard logic if invalid index or if current item is a manual queue item
                    oldCurrentIndex == C.INDEX_UNSET ||
                    currentMediaItem?.mediaMetadata?.extras?.getBoolean("IS_MANUAL_QUEUE") == true
                ) {
                    seekAction()
                    return
                }

                //Measure manual queue
                var manualQueueSize = 0
                for (i in oldCurrentIndex + 1 until mediaItemCount) {
                    if (getMediaItemAt(i).mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true)
                        manualQueueSize++
                    else break
                }

                //Perform seek
                seekAction()

                //Only move manual queue if it isn't empty
                if (manualQueueSize > 0) {
                    val queueItems = mutableListOf<MediaItem>()
                    val startIndex = oldCurrentIndex + 1
                    val endIndex = startIndex + manualQueueSize

                    //Extract items
                    for (i in startIndex until endIndex) {
                        queueItems.add(getMediaItemAt(i))
                    }

                    //Erase them from their old position (removeMediaItems is exclusive of the end index)
                    removeMediaItems(startIndex, endIndex)

                    //Insert them right after the current item's location
                    addMediaItems(currentMediaItemIndex + 1, queueItems)
                }
            }
        }

        //Initialize MediaSession and link it to the player
        mediaSession = MediaSession.Builder(this, forwardingPlayer).build()
    }

    //Rebuild timeline with saved playback state (upon app launch)
    private fun restoreStateOnStart() {
        //Launch on Main thread - Data fetches are already on IO thread
        serviceScope.launch {
            //Get entire saved timeline
            val timelineItems = persistenceRepository.getRestoredMediaItems()
            //Get entire saved original playlist
            val originalItems = persistenceRepository.getRestoredOriginalPlaylist()
            //Get saved player data states
            val meta = withContext(Dispatchers.IO) { persistenceRepository.playbackMetadata.first() }

            if (timelineItems.isNotEmpty()) {
                //Update lists and states
                playbackStateStore.originalPlaylist = originalItems
                playbackStateStore.setPlaylistId(meta.playlistId)
                playbackStateStore.setShuffling(meta.isShuffling)

                //Rebuild ExoPlayer's timeline
                exoPlayer.setMediaItems(timelineItems)
                //Apply saved repeat mode
                exoPlayer.repeatMode = if (meta.repeatingCurrent) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

                //Seek to saved index and duration (pick up where user left off)
                val targetIndex = if (meta.index >= 0 && meta.index < timelineItems.size) meta.index else 0
                exoPlayer.seekTo(targetIndex, meta.position) //Perform seek
                exoPlayer.prepare()
            }
        }
    }

    private fun restartPlaylistNatively(mediaItems: List<MediaItem>, playlistId: Int, startShuffled: Boolean) {
        //Prepare final timeline with shared prep logic from Store
        val (finalTimeline, playIndex) = playbackStateStore.preparePlaylistTimeline(
            mediaItems = mediaItems,
            playlistId = playlistId,
            startItemIndex = -1,
            startShuffled = startShuffled
        )

        //Apply timeline to ExoPlayer
        exoPlayer.setMediaItems(finalTimeline)
        //Seek to specified start item (always 0 in this case)
        exoPlayer.seekTo(playIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    //Erase all manual queue items from timeline BEFORE current item
    private fun consumePastManualQueueItems(player: Player) {
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

    //Save all current player data (timeline, states, lists) to disk
    private fun saveCurrentStateToDisk(player: Player) {
        //Capture snapshot on Main thread
        val position = player.currentPosition
        val index = player.currentMediaItemIndex
        val isShuffling = playbackStateStore.isShuffling.value
        val isRepeating = player.repeatMode == Player.REPEAT_MODE_ONE
        val playlistId = playbackStateStore.currentPlaylistId.value
        val originalCopy = playbackStateStore.originalPlaylist.toList()

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
    private fun savePositionOnly(player: Player) {
        //Capture snapshot on main thread
        val index = player.currentMediaItemIndex
        val position = player.currentPosition

        //Perform save on IO thread
        serviceScope.launch(Dispatchers.IO) {
            persistenceRepository.savePlaybackPosition(index, position)
        }
    }

    //Widget updating methods
    private fun updateWidget(player: Player) {
        val mediaItem = player.currentMediaItem
        val isPlaying = player.isPlaying
        val hasTimelineItems = player.mediaItemCount > 0

        val artworkUri = mediaItem?.mediaMetadata?.artworkUri?.toString() ?: ""
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Title"
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist"

        //Execute DataStore operations on background thread
        widgetScope.launch {
                //If the current item is not null, update the widget
                if (mediaItem != null) {
                    updateWidgetState {
                        this[PlayerWidget.ImageUriKey] = artworkUri
                        this[PlayerWidget.TitleKey] = title
                        this[PlayerWidget.ArtistKey] = artist
                        this[PlayerWidget.IsPlayingKey] = isPlaying
                    }
                }
                //If the current item is null but the controller is still active and the timeline is empty,
                //Then the app is open but there is nothing currently playing
                else if (!hasTimelineItems) {
                    updateWidgetState {
                        this[PlayerWidget.ImageUriKey] = ""
                        this[PlayerWidget.TitleKey] = "Not Playing"
                        this[PlayerWidget.ArtistKey] = ""
                        this[PlayerWidget.IsPlayingKey] = false
                    }
                }
                //If the current item is null and the controller is disconnected/closing,
                //Then preserve the last media item's information on the home screen and force the icon to "Play"
                else {
                    updateWidgetState {
                        this[PlayerWidget.IsPlayingKey] = false
                    }
                }
        }
    }

    private suspend fun updateWidgetState(transform: MutablePreferences.() -> Unit) {
        withContext(Dispatchers.IO) {
            val glanceManager = GlanceAppWidgetManager(this@PlaybackService)
            val glanceIds = glanceManager.getGlanceIds(PlayerWidget::class.java)
            val widgetInstance = PlayerWidget()

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(this@PlaybackService, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply(transform)
                }
                widgetInstance.update(this@PlaybackService, glanceId)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        //Force widget to a paused state to update PlayerWidget before the service shuts down
        runBlocking {
            updateWidgetState {
                this[PlayerWidget.IsPlayingKey] = false
            }
        }

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        widgetScope.cancel()
        super.onDestroy()
    }
}