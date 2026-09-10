package com.devball.jubalaudio.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.media3.common.MediaItem
import com.devball.jubalaudio.data.local.OriginalPlaylistEntity
import com.devball.jubalaudio.data.local.PlaybackQueueDao
import com.devball.jubalaudio.data.local.PlaybackQueueEntity
import com.devball.jubalaudio.data.local.asManualQueueItem
import com.devball.jubalaudio.data.local.toMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackPersistenceRepository @Inject constructor(
    private val playbackDao: PlaybackQueueDao,
    private val mediaRepository: MediaRepository,
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    private val saveMutex = Mutex()

    private object Keys {
        val LAST_POSITION = longPreferencesKey("last_position")
        val CURRENT_INDEX = intPreferencesKey("current_index")
        val SHUFFLE_ON = booleanPreferencesKey("shuffle_on")
        val REPEATING_CURRENT = booleanPreferencesKey("repeating_current")
        val CURRENT_PLAYLIST_ID = intPreferencesKey("current_playlist_id")
    }


    suspend fun savePlaybackState(
        position: Long,
        index: Int,
        isShuffling: Boolean,
        isRepeating: Boolean,
        playlistId: Int?,
        timelineItems: List<MediaItem>,
        originalItems: List<MediaItem>
    ) = withContext(Dispatchers.IO) {
        saveMutex.withLock { //Prevent race conditions
            //Save metadata to DataStore
            dataStore.edit { prefs ->
                prefs[Keys.LAST_POSITION] = position
                prefs[Keys.CURRENT_INDEX] = index
                prefs[Keys.SHUFFLE_ON] = isShuffling
                prefs[Keys.REPEATING_CURRENT] = isRepeating
                if (playlistId != null) prefs[Keys.CURRENT_PLAYLIST_ID] = playlistId
                else prefs.remove(Keys.CURRENT_PLAYLIST_ID)
            }

            //Atomically save timeline and original playlist to Room
            playbackDao.replaceQueue(timelineItems.toQueueEntities())
            playbackDao.replaceOriginalPlaylist(originalItems.toOriginalEntities())

        }
    }

    suspend fun savePlaybackPosition(index: Int, position: Long) = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENT_INDEX] = index
            prefs[Keys.LAST_POSITION] = position
        }
    }

    suspend fun getRestoredMediaItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val savedQueue = playbackDao.getSavedQueue()
        savedQueue.mapNotNull { entry ->
            val entity = mediaRepository.getMediaById(entry.mediaId) ?: return@mapNotNull null
            val mediaItem = entity.toMediaItem()

            //If it was a manual queue item, rebuild with same extension function to tag as manual
            if (entry.isManual) mediaItem.asManualQueueItem() else mediaItem
        }
    }

    suspend fun getRestoredOriginalPlaylist(): List<MediaItem> = withContext(Dispatchers.IO) {
        playbackDao.getOriginalPlaylist().mapNotNull { entry ->
            mediaRepository.getMediaById(entry.mediaId)?.toMediaItem()
        }
    }

    suspend fun clearPlaybackState() = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            //Clear playback metadata from DataStore
            dataStore.edit { prefs ->
                prefs.remove(Keys.LAST_POSITION)
                prefs.remove(Keys.CURRENT_INDEX)
                prefs.remove(Keys.SHUFFLE_ON)
                prefs.remove(Keys.REPEATING_CURRENT)
                prefs.remove(Keys.CURRENT_PLAYLIST_ID)
            }

            //Clear lists from Room tables
            playbackDao.clearQueue()
            playbackDao.clearOriginalPlaylist()
        }
    }

    val playbackMetadata = dataStore.data.map { prefs ->
        PlaybackMetadata(
            position = prefs[Keys.LAST_POSITION] ?: 0L,
            index = prefs[Keys.CURRENT_INDEX] ?: 0,
            isShuffling = prefs[Keys.SHUFFLE_ON] ?: false,
            repeatingCurrent = prefs[Keys.REPEATING_CURRENT] ?: false,
            playlistId = prefs[Keys.CURRENT_PLAYLIST_ID]
        )
    }
}

data class PlaybackMetadata(
    val position: Long,
    val index: Int,
    val isShuffling: Boolean,
    val repeatingCurrent: Boolean,
    val playlistId: Int?
)

//Extension helpers for mapping
private fun List<MediaItem>.toQueueEntities() = mapIndexedNotNull { i, item ->
    val id = item.mediaMetadata.extras?.getString("ORIGINAL_MEDIA_ID")?.toIntOrNull()
        ?: item.mediaId.toIntOrNull()
        ?: return@mapIndexedNotNull null
    val isManual = item.mediaMetadata.extras?.getBoolean("IS_MANUAL_QUEUE") == true
    PlaybackQueueEntity(mediaId = id, isManual = isManual, sequenceOrder = i)
}

private fun List<MediaItem>.toOriginalEntities() = mapIndexedNotNull { i, item ->
    val id = item.mediaId.toIntOrNull() ?: return@mapIndexedNotNull null
    OriginalPlaylistEntity(mediaId = id, sequenceOrder = i)
}
