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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer

    //Coroutine scope dedicated to updating PlayerWidget
    private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)

            //Trigger widget updates whenever playback state, timeline, or current item changes
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED
            )) { updateWidget(player) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
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