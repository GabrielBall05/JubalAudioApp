package com.devball.jubalaudio.player

import androidx.annotation.OptIn
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

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
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
                super.seekToNext()
                play()
            }

            //Intercept absolute commands to seek to next media item
            override fun seekToNextMediaItem() {
                super.seekToNextMediaItem()
                play()
            }

            //Intercept standard Previous button clicks
            override fun seekToPrevious() {
                //Only execute custom seekToPrevious logic if player is actually seeking to previous
                if ((currentPosition <= maxSeekToPreviousPosition) && hasPreviousMediaItem()) {
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
                    shiftQueueAndSeek { super.seekToPreviousMediaItem() }
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

    //Gets called when a UI wants to connect to the player
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}