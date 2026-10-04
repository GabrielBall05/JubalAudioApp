package com.devball.jubalaudio.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
        val INITIAL_INFINITE_PLAYBACK = true
    }

    //Keys
    private object Keys {
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
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

    //Update Infinite Playback Setting
    suspend fun setInfinitePlayback(enabled: Boolean) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[Keys.INFINITE_PLAYBACK] = enabled
        }
    }
}