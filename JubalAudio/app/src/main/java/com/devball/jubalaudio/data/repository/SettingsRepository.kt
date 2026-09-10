package com.devball.jubalaudio.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devball.jubalaudio.util.MediaSortOrder
import com.devball.jubalaudio.util.PlaylistsSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    //Initial Settings
    @Suppress("MayBeConstant")
    companion object {
        val INITIAL_KEEP_SCREEN_ON = true
        val INITIAL_MEDIA_SORT_ORDER = MediaSortOrder.DATE_ADDED_MOST_RECENT
        val INITIAL_PLAYLISTS_SORT_ORDER = PlaylistsSortOrder.DATE_CREATED_MOST_RECENT
        val INITIAL_INFINITE_PLAYBACK = true
    }

    //Keys
    private object Keys {
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DEFAULT_MEDIA_SORT_ORDER = stringPreferencesKey("default_media_sort_order")
        val DEFAULT_PLAYLISTS_SORT_ORDER = stringPreferencesKey("default_playlists_sort_order")
        val INFINITE_PLAYBACK = booleanPreferencesKey("infinite_playback")
    }


    //Keep Screen On (Flow)
    val keepScreenOnFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[Keys.KEEP_SCREEN_ON] ?: INITIAL_KEEP_SCREEN_ON
        }

    //Default Sort Order for Media in Home Screen (Flow)
    val defaultMediaSortOrderFlow: Flow<MediaSortOrder> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val sortOrderName = preferences[Keys.DEFAULT_MEDIA_SORT_ORDER] ?: INITIAL_MEDIA_SORT_ORDER.name
            runCatching {
                MediaSortOrder.valueOf(sortOrderName)
            }.getOrDefault(INITIAL_MEDIA_SORT_ORDER)
        }

    //Default Sort Order for Playlists (Flow)
    val defaultPlaylistsSortOrderFlow: Flow<PlaylistsSortOrder> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val sortOrderName = preferences[Keys.DEFAULT_PLAYLISTS_SORT_ORDER] ?: INITIAL_PLAYLISTS_SORT_ORDER.name
            runCatching {
                PlaylistsSortOrder.valueOf(sortOrderName)
            }.getOrDefault(INITIAL_PLAYLISTS_SORT_ORDER)
        }

    //Infinite Playback (Flow)
    val infinitePlaybackFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[Keys.INFINITE_PLAYBACK] ?: INITIAL_INFINITE_PLAYBACK
        }


    //Update Keep Screen On Setting
    suspend fun setKeepScreenOn(keepOn: Boolean) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[Keys.KEEP_SCREEN_ON] = keepOn
        }
    }

    //Update Default Media Sort Order Setting
    suspend fun setDefaultMediaSortOrder(sortOrder: MediaSortOrder) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_MEDIA_SORT_ORDER] = sortOrder.name
        }
    }

    //Update Default Playlists Sort Order Setting
    suspend fun setDefaultPlaylistsSortOrder(sortOrder: PlaylistsSortOrder) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_PLAYLISTS_SORT_ORDER] = sortOrder.name
        }
    }

    //Update Infinite Playback Setting
    suspend fun setInfinitePlayback(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[Keys.INFINITE_PLAYBACK] = enabled
        }
    }
}