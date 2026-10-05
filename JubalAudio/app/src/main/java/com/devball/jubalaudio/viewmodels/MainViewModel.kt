package com.devball.jubalaudio.viewmodels

import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.data.local.entity.MediaEntity
import com.devball.jubalaudio.data.local.entity.PlaylistEntity
import com.devball.jubalaudio.data.local.toMediaItem
import com.devball.jubalaudio.data.repository.MediaRepository
import com.devball.jubalaudio.data.repository.PlaylistRepository
import com.devball.jubalaudio.data.repository.SettingsRepository
import com.devball.jubalaudio.delegates.MediaActionsDelegate
import com.devball.jubalaudio.delegates.PlaylistActionsDelegate
import com.devball.jubalaudio.player.MediaControllerManager
import com.devball.jubalaudio.viewmodels.events.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val controllerManager: MediaControllerManager,
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    mediaActionsDelegate: MediaActionsDelegate,
    playlistActionsDelegate: PlaylistActionsDelegate,
    settingsRepository: SettingsRepository
): BaseViewModel(),
    MediaActionsDelegate by mediaActionsDelegate,
    PlaylistActionsDelegate by playlistActionsDelegate {

    private var playbackJob: Job? = null

    //Expose states from the manager
    val currentMediaItem = controllerManager.currentMediaItem
    val previousMediaItem = controllerManager.previousMediaItem
    val isPlaying = controllerManager.isPlaying
    val currentPosition = controllerManager.currentPosition
    val duration = controllerManager.duration
    val isRepeatingCurrent = controllerManager.repeatingCurrent
    val isShuffleModeEnabled = controllerManager.isShuffling
    val manualQueue = controllerManager.manualQueueState
    val upNext = controllerManager.upNextState
    val currentPlaylistId = controllerManager.currentPlaylistId

    //Full db entry for the currently playing media item
    private val _currentMediaEntity = MutableStateFlow<MediaEntity?>(null)
    val currentMediaEntity = _currentMediaEntity.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPlaylist: StateFlow<PlaylistEntity?> = currentPlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else playlistRepository.getPlaylistById(id)
        }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val isCurrentMediaInCurrentPlaylist: StateFlow<Boolean> = combine(
        currentMediaEntity,
        currentPlaylistId
    ) { media, playlistId ->
        if (playlistId == null || media == null) flowOf(false)
        else playlistRepository.isMediaInPlaylist(media.mediaId, playlistId)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    //Expose states from settings
    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOnFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.INITIAL_KEEP_SCREEN_ON
    )
    val infinitePlayback: StateFlow<Boolean> = settingsRepository.infinitePlaybackFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.INITIAL_KEEP_SCREEN_ON
    )

    init {
        //Bind delegates
        bindMediaActions(this)
        bindPlaylistActions(this)

        //Ensure the controller is connected when the app starts or reopens
        controllerManager.setupController()

        //Watch the isPlaying state to toggle ticker
        viewModelScope.launch {
            isPlaying.collect { playing ->
                if (playing) startPlaybackTicker()
                else stopPlaybackTicker()
            }
        }

        //Get currently playing media entity
        viewModelScope.launch {
            currentMediaItem.collect { mediaItem ->
                //Extract real media id because if it is a manual queue item, it is in the extras
                val id = mediaItem?.mediaMetadata?.extras?.getString("ORIGINAL_MEDIA_ID")?.toIntOrNull()
                    ?: mediaItem?.mediaId?.toIntOrNull()
                //Update current media entity flow with actual Room entry
                _currentMediaEntity.value = id?.let { mediaRepository.getMediaById(it) }
            }
        }

        //Watch for player error messages for UI events
        viewModelScope.launch {
            controllerManager.errorMessage.collect { message ->
                showToast(message)
            }
        }
    }


    //Player actions
    fun togglePlayPause() = controllerManager.togglePlayPause()
    fun seekToNext() = controllerManager.seekToNext()
    fun seekToPrevious(ensureFullSeek: Boolean = false) = controllerManager.seekToPrevious(ensureFullSeek)
    fun seekTo(positionMs: Long) = controllerManager.seekTo(positionMs)
    fun toggleShuffle() = controllerManager.toggleShuffle()
    fun toggleRepeatMode() = controllerManager.toggleRepeatMode()

    fun playPlaylist(playlistId: Int, startItemId: Int? = null) {
        viewModelScope.launch {
            val mediaList = playlistRepository.fetchPlaylistMediaList(playlistId)
                .filter { !it.isStaleUri }
            val startItemIndex = mediaList.indexOfFirst { it.mediaId == startItemId }
            withContext(Dispatchers.Main) { //MediaController must use Main thread
                controllerManager.playPlaylist(
                    mediaItems = mediaList.map { it.toMediaItem() },
                    playlistId = playlistId,
                    startItemIndex = startItemIndex,
                    startShuffled = isShuffleModeEnabled.value //Use current shuffle preference
                )
            }
        }
    }

    fun addPlaylistToQueue(playlistId: Int) {
        viewModelScope.launch {
            val mediaItems = playlistRepository.fetchPlaylistMediaList(playlistId)
                .filter { !it.isStaleUri }
                .map { it.toMediaItem() }
            withContext(Dispatchers.Main) { //MediaController must use Main thread
                controllerManager.addToQueue(mediaItems)
                showToast("Added playlist to queue")
            }
        }
    }

    fun addMediaToQueue(mediaList: List<MediaEntity>) {
        controllerManager.addToQueue(mediaList
            .filter { !it.isStaleUri }
            .map { it.toMediaItem() })
        showToast("Added ${mediaList.size} item${if (mediaList.size > 1) "s" else ""} to queue")
    }

    fun playMediaNow(media: MediaEntity) {
        if (!media.isStaleUri) controllerManager.playNow(media.toMediaItem())
    }

    fun clearQueue() = controllerManager.clearQueue()
    fun moveManualQueueItem(fromIndex: Int, toIndex: Int) = controllerManager.moveManualQueueItem(fromIndex, toIndex)
    fun moveUpNextItem(fromIndex: Int, toIndex: Int) = controllerManager.moveUpNextItem(fromIndex, toIndex)

    fun manualQueueSkipToIndex(index: Int) = controllerManager.manualQueueSkipToIndex(index)
    fun upNextSkipToIndex(index: Int) = controllerManager.upNextSkipToIndex(index)
    fun removeItemAtIndex(index: Int, isManual: Boolean) = controllerManager.removeItemAtIndex(index, isManual)

    //Ticker
    private fun startPlaybackTicker() {
        playbackJob?.cancel() //Clear any existing job
        playbackJob = viewModelScope.launch {
            while (true) {
                controllerManager.updateCurrentPosition()
                delay(500L) //Tick every 500ms (2 tps)
            }
        }
    }

    private fun stopPlaybackTicker() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun clearPlayer() {
        controllerManager.nukePlayer()
    }

    //Clean up controller when app truly closes
    override fun onCleared() {
        super.onCleared()
        controllerManager.releaseController()
    }
}
