package com.devball.jubalaudio.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
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

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    //Keys
    private object Keys {
        val MEDIA_SORT_ORDER = stringPreferencesKey("media_sort_order")
        val PLAYLISTS_SORT_ORDER = stringPreferencesKey("playlists_sort_order")
    }

    //Initial Preferences
    @Suppress("MayBeConstant")
    companion object {
        val INITIAL_MEDIA_SORT_ORDER = MediaSortOrder.DATE_ADDED_MOST_RECENT
        val INITIAL_PLAYLISTS_SORT_ORDER = PlaylistsSortOrder.DATE_CREATED_MOST_RECENT
    }

    //Flows
    val mediaSortOrderFlow: Flow<MediaSortOrder> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            runCatching {
                MediaSortOrder.valueOf(preferences[Keys.MEDIA_SORT_ORDER] ?: INITIAL_MEDIA_SORT_ORDER.name)
            }
            .getOrDefault(INITIAL_MEDIA_SORT_ORDER)
        }

    val playlistsSortOrderFlow: Flow<PlaylistsSortOrder> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            runCatching {
                PlaylistsSortOrder.valueOf(preferences[Keys.PLAYLISTS_SORT_ORDER] ?: INITIAL_PLAYLISTS_SORT_ORDER.name)
            }
            .getOrDefault(INITIAL_PLAYLISTS_SORT_ORDER)
        }

    //Update methods
    suspend fun updateMediaSortOrder(sortOrder: MediaSortOrder) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[Keys.MEDIA_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updatePlaylistsSortOrder(sortOrder: PlaylistsSortOrder) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[Keys.PLAYLISTS_SORT_ORDER] = sortOrder.name
        }
    }
}