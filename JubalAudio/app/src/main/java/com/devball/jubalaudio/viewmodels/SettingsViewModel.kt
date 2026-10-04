package com.devball.jubalaudio.viewmodels

import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
): BaseViewModel() {
    //Combine all flows into a single source for the UI
    val uiState: StateFlow<SettingsUIState> = combine(
        settingsRepository.keepScreenOnFlow,
        settingsRepository.infinitePlaybackFlow
    ) { screenOn, infinitePlayback ->
        SettingsUIState(
            keepScreenOn = screenOn,
            infinitePlayback = infinitePlayback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUIState() //Start with universal defaults
    )


    //Explicit auto-save functions
    fun setKeepScreenOn(newSetting: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(newSetting)
        }
    }

    fun setInfinitePlayback(newSetting: Boolean) {
        viewModelScope.launch {
            settingsRepository.setInfinitePlayback(newSetting)
        }
    }
}

data class SettingsUIState(
    val keepScreenOn: Boolean = SettingsRepository.INITIAL_KEEP_SCREEN_ON,
    val infinitePlayback: Boolean = SettingsRepository.INITIAL_INFINITE_PLAYBACK
)