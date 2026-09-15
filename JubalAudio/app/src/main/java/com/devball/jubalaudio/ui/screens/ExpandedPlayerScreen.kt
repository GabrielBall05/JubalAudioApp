package com.devball.jubalaudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devball.jubalaudio.ui.components.common.ItemInfoColumn
import com.devball.jubalaudio.ui.components.common.SurfacedImage
import com.devball.jubalaudio.ui.components.dialogs.ConfirmationDialog
import com.devball.jubalaudio.ui.components.dialogs.PlaylistFormDialog
import com.devball.jubalaudio.ui.components.dialogs.PlaylistPicker
import com.devball.jubalaudio.ui.components.optionsheets.MediaOption
import com.devball.jubalaudio.ui.components.optionsheets.MediaOptionsSheetContent
import com.devball.jubalaudio.ui.viewmodels.MainViewModel
import com.devball.jubalaudio.util.KeepScreenOn
import com.devball.jubalaudio.util.indicatorBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerScreen(
    viewModel: MainViewModel,
    onCollapse: () -> Unit
) {
    //Collect settings states from viewmodel
    val infinitePlayback by viewModel.infinitePlayback.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

    if (keepScreenOn) KeepScreenOn() //Call helper composable to ensure the screen stays on while this screen/composable is active

    //Collect states from viewmodel
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val currentMediaEntity by viewModel.currentMediaEntity.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isRepeatingCurrent by viewModel.isRepeatingCurrent.collectAsStateWithLifecycle()
    val isShuffleModeEnabled by viewModel.isShuffleModeEnabled.collectAsStateWithLifecycle()
    val manualQueue by viewModel.manualQueue.collectAsStateWithLifecycle()
    val upNext by viewModel.upNext.collectAsStateWithLifecycle()
    val availablePlaylists by viewModel.availablePlaylists.collectAsStateWithLifecycle()
    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val isCurrentMediaInCurrentPlaylist by viewModel.isCurrentMediaInCurrentPlaylist.collectAsStateWithLifecycle()

    var showQueueScreen by rememberSaveable { mutableStateOf(false) }
    var showMediaItemMenu by rememberSaveable { mutableStateOf(false) }
    var showPlaylistPicker by rememberSaveable { mutableStateOf(false) }
    var showPlaylistForm by rememberSaveable { mutableStateOf(false) }
    var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }

    //Sheet states
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val optionsSheetState = rememberModalBottomSheetState()

    //Slider states
    var sliderPosition by remember { mutableFloatStateOf(0F) }
    var isDragging by remember { mutableStateOf(false) }

    //Sync slider with actual position unless user is dragging it
    LaunchedEffect(currentPosition) { if (!isDragging) sliderPosition = currentPosition.toFloat() }

    //Refresh playlists when the picker is opened for the current media item
    LaunchedEffect(showPlaylistPicker, currentMediaEntity) {
        if (showPlaylistPicker) currentMediaEntity?.let { viewModel.refreshAvailablePlaylists(it.mediaId) }
    }


    //Screen UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 100.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            //Back button, Title, Options menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Back button
                IconButton(onClick = onCollapse) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse"
                    )
                }

                //Current Playlist Name
                Text(
                    modifier = Modifier.weight(1f),
                    text = currentPlaylist?.name ?: "Ad Hoc",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                //Options Menu
                IconButton(onClick = { showMediaItemMenu = true }) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options"
                    )
                }
            }

            //Artwork
            SurfacedImage(
                model = currentMediaItem?.mediaMetadata?.artworkUri?.toString(),
                contentDescription = "Artwork Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1f),
            )

            //Information + Slider + Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                //Title + Creator + Repeat Mode + Add To Playlist Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ItemInfoColumn(
                        paddingValues = PaddingValues(start = 6.dp),
                        line1 = { Text(
                            text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Title",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        ) },
                        line2 = { Text(
                            text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Creator",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        ) }
                    )

                    //Repeat Button
                    IconButton(
                        modifier = Modifier
                            .size(48.dp)
                            .indicatorBorder(enabled = isRepeatingCurrent, color = MaterialTheme.colorScheme.primary)
                            .clip(CircleShape)
                            .aspectRatio(1f),
                        onClick = { viewModel.toggleRepeatMode() }
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            imageVector = Icons.Default.Repeat,
                            tint = if (isRepeatingCurrent) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.75f),
                            contentDescription = "Repeat Mode"
                        )
                    }
                }

                //Slider + Times
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        //Seek Slider
                        Slider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp),
                            value = sliderPosition,
                            onValueChange = {
                                isDragging = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                viewModel.seekTo(sliderPosition.toLong())
                            },
                            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                            thumb = {
                                Box(modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(4.dp),
                                    thumbTrackGapSize = 0.dp,
                                    drawStopIndicator = null
                                )
                            }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-10).dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            //Duration Texts
                            Text(text = formatTime(sliderPosition.toLong()), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text(text = formatTime(duration), style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.End)
                        }
                    }
                }

                //Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Shuffle Toggle Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .indicatorBorder(enabled = isShuffleModeEnabled, color = MaterialTheme.colorScheme.primary)
                            .clip(CircleShape)
                            .clickable { viewModel.toggleShuffle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            imageVector = Icons.Rounded.Shuffle,
                            tint = if(isShuffleModeEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.75f),
                            contentDescription = "Shuffle Toggle"
                        )
                    }

                    //Grouped Playback Controls (Prev, Toggle, Next)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        //Previous Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.seekToPrevious() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous"
                            )
                        }

                        //Play/Pause Toggle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null //Remove ripple effect when clicked
                                ) { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play/Pause Toggle"
                            )
                        }

                        //Next Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.seekToNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next"
                            )
                        }
                    }

                    //Queue Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { showQueueScreen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Open Queue"
                        )
                    }
                }
            }
        }

        //Show Queue screen if user hits the queue button
        if (showQueueScreen) {
            ModalBottomSheet(
                onDismissRequest = { showQueueScreen = false },
                sheetState = queueSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)) {
                    QueueScreen(
                        infinitePlaybackSetting = infinitePlayback,
                        activePlaylist = currentPlaylist,
                        currentlyPlaying = currentMediaItem,
                        manualQueue = manualQueue,
                        upNext = upNext,
                        onDismiss = { showQueueScreen = false },
                        onClearQueue = { viewModel.clearQueue() },
                        onMoveManualQueueItem = { from, to ->
                            viewModel.moveManualQueueItem(from, to)
                        },
                        onMoveUpNextItem = { from, to ->
                            viewModel.moveUpNextItem(from, to)
                        },
                        onManualQueueSkipToIndex = { viewModel.manualQueueSkipToIndex(it) },
                        onUpNextSkipToIndex = { viewModel.upNextSkipToIndex(it) },
                        onRemoveItemAtIndex = { index, isManual ->
                            viewModel.removeItemAtIndex(index, isManual)
                        }
                    )
                }
            }
        }

        //Show options menu if user hits ellipsis and the media entity is available
        if (showMediaItemMenu) currentMediaEntity?.let { media ->
            ModalBottomSheet(
                onDismissRequest = { showMediaItemMenu = false },
                sheetState = optionsSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MediaOptionsSheetContent(
                    media = media,
                    showEditOption = false,
                    showPlayOption = false,
                    showRemoveOption = isCurrentMediaInCurrentPlaylist,
                    onOptionClick = { option ->
                        showMediaItemMenu = false
                        when (option) {
                            MediaOption.EDIT -> { /* TODO: MAYBE IMPLEMENT - OR DON'T, IDK */ }
                            MediaOption.PLAY_NOW -> { /* Not used in Expanded Player Screen (already playing) */ }
                            MediaOption.ADD_TO_QUEUE -> { viewModel.addMediaToQueue(listOf(media)) }
                            MediaOption.ADD_TO_PLAYLIST -> { showPlaylistPicker = true }
                            MediaOption.REMOVE_FROM_PLAYLIST -> { showRemoveConfirmation = true }
                            MediaOption.DELETE -> { /* Not used in Expanded Player Screen */ }
                        }
                    }
                )
            }
        }

        //Show playlist picker if user hits add to playlist
        if (showPlaylistPicker) currentMediaEntity?.let { media ->
            PlaylistPicker(
                playlists = availablePlaylists,
                onDismiss = { showPlaylistPicker = false },
                onCreateClick = {
                    showPlaylistPicker = false
                    showPlaylistForm = true
                },
                onConfirm = { selectedIds ->
                    viewModel.addMediaToPlaylists(media.mediaId, selectedIds)
                    showPlaylistPicker = false
                }
            )
        }

        //Show playlist form if user hits create playlist from playlist picker
        if (showPlaylistForm) {
            PlaylistFormDialog(
                onDismiss = {
                    showPlaylistForm = false
                    showPlaylistPicker = true
                },
                onConfirm = { newPlaylist ->
                    currentMediaEntity?.let { media ->
                        viewModel.createPlaylist(newPlaylist, media.mediaId)
                    }
                    showPlaylistForm = false
                    showPlaylistPicker = true
                }
            )
        }

        //Show confirmation dialog if user hits remove from playlist and both the current media entity and playlist is available
        if (showRemoveConfirmation) currentMediaEntity?.let { media ->
            currentPlaylist?.let { playlist ->
                ConfirmationDialog(
                    title = "Are you sure you want to remove ${media.title} from \"${playlist.name}\"?",
                    text = "You can always re-add it",
                    confirmText = "Remove",
                    onDismiss = { showRemoveConfirmation = false },
                    onConfirm = {
                        viewModel.removeFromPlaylist(media.mediaId, playlist.playlistId)
                        showRemoveConfirmation = false
                    }
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
}