package com.example.offlineplayer.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.ui.components.common.BulkActionsBar
import com.example.offlineplayer.ui.components.common.EmptyMessage
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.components.common.SurfacedImage
import com.example.offlineplayer.ui.components.dialogs.ConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaBulkDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaDialog
import com.example.offlineplayer.ui.components.dialogs.LoadingDialog
import com.example.offlineplayer.ui.components.dialogs.MediaPicker
import com.example.offlineplayer.ui.components.dialogs.PlaylistFormDialog
import com.example.offlineplayer.ui.components.dialogs.PlaylistPicker
import com.example.offlineplayer.ui.components.listitems.MediaListItemReorderable
import com.example.offlineplayer.ui.components.listitems.MediaListItemSelectable
import com.example.offlineplayer.ui.components.listitems.MediaListItemStandard
import com.example.offlineplayer.ui.components.listitems.StaleUriListItem
import com.example.offlineplayer.ui.components.optionsheets.MediaOption
import com.example.offlineplayer.ui.components.optionsheets.MediaOptionsSheetContent
import com.example.offlineplayer.ui.components.optionsheets.PlaylistOption
import com.example.offlineplayer.ui.components.optionsheets.PlaylistOptionsSheet
import com.example.offlineplayer.ui.viewmodels.PlaylistDetailsViewModel
import com.example.offlineplayer.util.ObserveUiEvents
import com.example.offlineplayer.util.SwipeDismissable
import com.example.offlineplayer.util.UiEvent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class TopBarState {
    Reordering, Selecting, Standard
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    viewModel: PlaylistDetailsViewModel = hiltViewModel(), //Let Hilt inject ViewModel
    onBack: () -> Unit,
    onPlayMediaClick: (MediaEntity) -> Unit,
    onAddToQueueClick: (List<MediaEntity>) -> Unit,
    onPlayPlaylistClick: (Int, Int?) -> Unit,
    onTogglePlayPauseClick: () -> Unit,
    isActivePlaylistPlaying: Boolean,
    activePlaylistId: Int?
) {
    //Ui Event Observer
    ObserveUiEvents(eventFlow = viewModel.uiEvent)

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val itemCount by viewModel.itemCount.collectAsStateWithLifecycle()
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val fullMediaList by viewModel.playlistMedia.collectAsStateWithLifecycle()
    val availablePlaylists by viewModel.availablePlaylists.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()
    val mediaMap = remember(mediaList) { mediaList.associateBy { it.mediaId } }

    var isReordering by rememberSaveable { mutableStateOf(false) }
    var idsToRemove by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var idsToAddToPlaylists by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var selectedMediaItemForMenu by rememberSaveable { mutableStateOf<MediaEntity?>(null) }
    var mediaToEdit by rememberSaveable { mutableStateOf<MediaEntity?>(null) }
    var idsToEdit by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var showMediaPicker by rememberSaveable { mutableStateOf(false) }
    var mediaNotInPlaylist by rememberSaveable { mutableStateOf<List<MediaEntity>>(emptyList()) }
    var isFetchingMedia by rememberSaveable { mutableStateOf(false) }
    var showPlaylistOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var editingPlaylist by rememberSaveable { mutableStateOf(false) }
    var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var showDeletePlaylistConfirmation by rememberSaveable { mutableStateOf(false) }
    var mediaIdToRelink by rememberSaveable { mutableStateOf<Int?>(null) }


    //Single File Picker Launcher
    val relinkLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { newUri -> mediaIdToRelink?.let { id ->
            viewModel.relinkMedia(id, newUri)
        } }
        mediaIdToRelink = null
    }

    //Jump to top of list when list size changes
    LaunchedEffect(mediaList.size) {
        if (mediaList.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    //Fetch media not in playlist when picker is shown
    LaunchedEffect(showMediaPicker) {
        if (showMediaPicker) {
            isFetchingMedia = true
            mediaNotInPlaylist = viewModel.getMediaNotInPlaylist()
            isFetchingMedia = false
        }
    }

    //Refresh available playlists when the selection for playlist addition is set
    LaunchedEffect(idsToAddToPlaylists, availablePlaylists.size) {
        if (idsToAddToPlaylists.isNotEmpty()) viewModel.refreshAvailablePlaylists(idsToAddToPlaylists)
    }


    //Screen UI
    SwipeDismissable(onDismiss = onBack) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                //Header (Back button, playlist details, menu button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Back Button
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }

                    //Cover Image, Name, Description, Item Count
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //Artwork
                        SurfacedImage(
                            model = playlist?.coverUri,
                            contentDescription = "Cover Image",
                            modifier = Modifier.clickable(onClick = { editingPlaylist = true }),
                            fallbackIcon = Icons.Default.LibraryMusic,
                            sizeInDp = 80.dp
                        )

                        //Playlist Details
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = playlist?.name ?: "Playlist Name", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                            playlist?.description?.let { description ->
                                Text(text = description, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                            Text(text = "$itemCount items", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        }
                    }

                    //Menu Button
                    IconButton(onClick = { showPlaylistOptionsSheet = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }
                }

                //Calculate state for top bar
                val topBarState = when {
                    isReordering -> TopBarState.Reordering
                    isAnySelected -> TopBarState.Selecting
                    else -> TopBarState.Standard
                }

                //Top Bar
                AnimatedContent(
                    targetState = topBarState,
                    label = "TopBarStateTransition",
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.95f))
                            .togetherWith(fadeOut(animationSpec = tween(180)))
                    }
                ) { targetState ->
                    when (targetState) {
                        //Reordering = Show Done Button
                        TopBarState.Reordering -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(all = 12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { isReordering = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp)
                                ) { Text("Done") }
                            }
                        }

                        //Selecting = Show Bulk Actions Bar
                        TopBarState.Selecting -> {
                            BulkActionsBar(
                                modifier = Modifier.padding(vertical = 12.dp),
                                isAnySelected = isAnySelected,
                                isAllSelected = isAllSelected,
                                onToggleAllClick = { viewModel.toggleSelectAll() },
                                onClearSelectionClick = { viewModel.clearSelection() }
                            ) {
                                IconButton(onClick = { idsToEdit = selectedIds.toList() }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { idsToAddToPlaylists = selectedIds.toList() }) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Another Playlist")
                                }
                                IconButton(onClick = { onAddToQueueClick(selectedIds.mapNotNull { id -> mediaMap[id] }) }) {
                                    Icon(Icons.Default.AddToQueue, contentDescription = "Add Selection to Queue")
                                }
                                IconButton(onClick = { idsToRemove = selectedIds.toList() }) {
                                    Icon(Icons.Default.PlaylistRemove, tint = MaterialTheme.colorScheme.error, contentDescription = "Remove From Playlist")
                                }
                            }
                        }

                        //Nothing = Show Search Bar + Play Button
                        TopBarState.Standard -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                //Search Bar
                                SearchBar(
                                    value = searchQuery,
                                    placeHolderText = "Search in playlist",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClear = { viewModel.onSearchQueryChange("") },
                                    onValueChange = { viewModel.onSearchQueryChange(it) }
                                )

                                //Play Button
                                IconButton(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(1f),
                                    onClick = {
                                        if (activePlaylistId == playlist?.playlistId) onTogglePlayPauseClick()
                                        else {
                                            playlist?.let { currentPlaylist ->
                                                onPlayPlaylistClick(currentPlaylist.playlistId, null)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            if (activePlaylistId == playlist?.playlistId && isActivePlaylistPlaying) Icons.Default.PauseCircle
                                            else Icons.Default.PlayCircle,
                                        contentDescription = "Play Playlist",
                                        modifier = Modifier.fillMaxSize(),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                //Media List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (fullMediaList.isEmpty()) {
                        item {
                            EmptyMessage(text = "You have no items in this playlist. Add some using the \"+\" button at the bottom-right of your screen, " +
                                    "or utilize the Home Screen's selecting options.")
                        }
                    } else if (mediaList.isEmpty()) {
                        item {
                            EmptyMessage(text = "No matches found.")
                        }
                    } else if (isReordering) {
                        //Use reorderable list item with the full media list
                        itemsIndexed(
                            items = fullMediaList,
                            key = { _, media -> media.mediaId }
                        ) { index, media ->
                            MediaListItemReorderable(
                                media = media,
                                isFirst = index == 0,
                                isLast = index == fullMediaList.size - 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        viewModel.moveMediaItemPosition(
                                            media.mediaId,
                                            fullMediaList[index - 1].mediaId
                                        )
                                    }
                                },
                                onMoveDown = {
                                    if (index < fullMediaList.size - 1) {
                                        viewModel.moveMediaItemPosition(
                                            media.mediaId,
                                            fullMediaList[index + 1].mediaId
                                        )
                                    }
                                }
                            )
                        }
                    } else {
                        //Use regular or selectable list item with the filtered list
                        items(
                            items = mediaList,
                            key = { it.mediaId }
                        ) { media ->
                            //Show stale list item if this media's uri is stale
                            if (media.isStaleUri) {
                                StaleUriListItem(
                                    media = media,
                                    onRelinkClick = {
                                        mediaIdToRelink = media.mediaId
                                        relinkLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onDeleteClick = { idsToRemove = listOf(media.mediaId) }
                                )
                            } else { //Otherwise show appropriate list item (viewing/selectable)
                                //Animate transition between viewing/selectable list items
                                AnimatedContent(
                                    targetState = isAnySelected,
                                    label = "MediaListItemTransition"
                                ) { animatingSelectionMode ->
                                    if (animatingSelectionMode) { //Use selectable list item if selecting
                                        MediaListItemSelectable(
                                            media = media,
                                            isSelected = selectedIds.contains(media.mediaId),
                                            onSelect = { viewModel.toggleSelection(media.mediaId) },
                                            constrainSelectToCheckbox = false,
                                            onMoreClick = { selectedMediaItemForMenu = it }
                                        )
                                    } else {
                                        MediaListItemStandard( //Use standard viewing list item if not selecting
                                            media = media,
                                            onImageClick = { playlist?.let { playlist ->
                                                onPlayPlaylistClick(playlist.playlistId, media.mediaId)
                                            } },
                                            onLongClick = { viewModel.toggleSelection(it.mediaId) },
                                            onMoreClick = { selectedMediaItemForMenu = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isAnySelected && !isReordering) {
                //Add Media To Playlist Button
                FloatingActionButton(
                    onClick = { showMediaPicker = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 16.dp)
                ) { Icon(Icons.Default.Add, contentDescription = "Add Media To Playlist") }
            }
        }
    }


    //Show ModalBottomSheet options for this playlist if user clicks the ellipsis at the top right
    if (showPlaylistOptionsSheet) {
        playlist?.let { currentPlaylist ->
            ModalBottomSheet(
                onDismissRequest = { showPlaylistOptionsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PlaylistOptionsSheet(
                    playlist = currentPlaylist,
                    showReorderOption = true,
                    onOptionClick = { option ->
                        showPlaylistOptionsSheet = false
                        when (option) {
                            PlaylistOption.EDIT -> editingPlaylist = true
                            PlaylistOption.PLAY_NOW -> onPlayPlaylistClick(currentPlaylist.playlistId, null)
                            PlaylistOption.ADD_TO_QUEUE -> onAddToQueueClick(fullMediaList)
                            PlaylistOption.ADD_MEDIA -> showMediaPicker = true
                            PlaylistOption.REORDER -> isReordering = true
                            PlaylistOption.DELETE -> showDeletePlaylistConfirmation = true
                        }
                    }
                )
            }
        }
    }

    //Show PlaylistFormDialog if user wants to edit this playlist
    if (editingPlaylist) {
        playlist?.let { currentPlaylist ->
            PlaylistFormDialog(
                playlistToEdit = currentPlaylist,
                onDismiss = { editingPlaylist = false },
                onConfirm = { plist ->
                    editingPlaylist = false
                    viewModel.editPlaylist(plist)
                }
            )
        }
    }

    //Show ConfirmationDialog if user wants to delete this playlist
    if (showDeletePlaylistConfirmation) {
        playlist?.let { currentPlaylist ->
            ConfirmationDialog(
                title = "Are you sure you want to delete the playlist \"${currentPlaylist.name}\"?",
                text = "This action cannot be undone",
                onDismiss = { showDeletePlaylistConfirmation = false },
                onConfirm = {
                    onBack()
                    viewModel.deletePlaylist(currentPlaylist)
                }
            )
        }
    }

    //Show ConfirmationDialog if user wants to remove media items from this playlist
    if (idsToRemove.isNotEmpty()) {
        playlist?.let { currentPlaylist ->
            val text = when {
                idsToRemove.size == 1 -> "\"${mediaList.first { it.mediaId == idsToRemove[0] }.title}\""
                else -> "these ${idsToRemove.size} items"
            }
            ConfirmationDialog(
                title = "Are you sure you want to remove $text from \"${currentPlaylist.name}\"?",
                text = "You can always re-add ${if (idsToRemove.size > 1) "them" else "it"}.",
                confirmText = "Remove",
                onDismiss = { idsToRemove = emptyList() },
                onConfirm = {
                    viewModel.removeMediaFromPlaylist(idsToRemove)
                    idsToRemove = emptyList()
                    viewModel.clearSelection()
                }
            )
        }
    }

    //Show ModalBottomSheet options for a media item if user clicks ellipsis on that item
    selectedMediaItemForMenu?.let { media ->
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItemForMenu = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MediaOptionsSheetContent(
                media = media,
                showRemoveOption = true,
                onOptionClick = { option ->
                    selectedMediaItemForMenu = null
                    when (option) {
                        MediaOption.EDIT -> mediaToEdit = media
                        MediaOption.PLAY_NOW -> onPlayMediaClick(media)
                        MediaOption.ADD_TO_QUEUE -> onAddToQueueClick(listOf(media))
                        MediaOption.ADD_TO_PLAYLIST -> idsToAddToPlaylists = listOf(media.mediaId)
                        MediaOption.REMOVE_FROM_PLAYLIST -> idsToRemove = listOf(media.mediaId)
                        MediaOption.DELETE -> { /* Not used in playlist details screen */ }
                    }
                }
            )
        }
    }

    //Show EditMediaDialog if user wants to edit a media item from here
    mediaToEdit?.let { media ->
        EditMediaDialog(
            media = media,
            onDismiss = { mediaToEdit = null },
            onConfirm = { updatedMedia ->
                mediaToEdit = null
                viewModel.updateMediaItem(updatedMedia)
            }
        )
    }

    //Show edit dialog if user hit edit (bulk)
    if (idsToEdit.isNotEmpty()) {
        if (idsToEdit.size == 1) {
            mediaToEdit = mediaList.first { it.mediaId == idsToEdit[0] }
            idsToEdit = emptyList()
        } else {
            EditMediaBulkDialog(
                itemCount = idsToEdit.size,
                commonCreator = viewModel.getCommonCreator(idsToEdit),
                commonArtwork = viewModel.getCommonArtwork(idsToEdit),
                onDismiss = { idsToEdit = emptyList() },
                onConfirmCreator = { viewModel.updateCreatorBulk(it, idsToEdit) },
                onConfirmArtwork = { viewModel.updateArtworkBulk(it, idsToEdit) }
            )
        }
    }

    //Show MediaPicker if user wants to add media to this playlist from here
    if (showMediaPicker && !isFetchingMedia) {
        playlist?.let { currentPlaylist ->
            MediaPicker(
                media = mediaNotInPlaylist.filter { !it.isStaleUri },
                onDismiss = { showMediaPicker = false },
                onConfirm = { mediaIds ->
                    showMediaPicker = false
                    viewModel.addMediaToPlaylists(mediaIds, listOf(currentPlaylist.playlistId))
                }
            )
        }
    }

    //Show PlaylistPicker if user wants to add items to another playlist from here
    if (idsToAddToPlaylists.isNotEmpty()) {
        PlaylistPicker(
            playlists = availablePlaylists,
            onCreateClick = { creatingPlaylist = true },
            onDismiss = { idsToAddToPlaylists = emptyList() },
            onConfirm = { playlistIds ->
                viewModel.addMediaToPlaylists(idsToAddToPlaylists, playlistIds)
                idsToAddToPlaylists = emptyList()
            }
        )
    }

    //Show PlaylistFormDialog if user clicked the Create Playlist shortcut in PlaylistPicker
    if (creatingPlaylist) {
        PlaylistFormDialog(
            onDismiss = { creatingPlaylist = false },
            onConfirm = { playlist ->
                viewModel.createPlaylist(playlist, idsToAddToPlaylists.ifEmpty { emptyList() })
                creatingPlaylist = false
            }
        )
    }

    //Show loading screen for potentially long operations
    if (isLoading) {
        LoadingDialog()
    }
}
