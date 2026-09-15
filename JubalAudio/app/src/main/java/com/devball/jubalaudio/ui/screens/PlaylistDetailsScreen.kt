package com.devball.jubalaudio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.common.BulkActionsBar
import com.devball.jubalaudio.ui.components.common.EmptyMessage
import com.devball.jubalaudio.ui.components.common.ItemInfoColumn
import com.devball.jubalaudio.ui.components.common.SearchBar
import com.devball.jubalaudio.ui.components.common.SurfacedImage
import com.devball.jubalaudio.ui.components.dialogs.ConfirmationDialog
import com.devball.jubalaudio.ui.components.dialogs.EditMediaBulkDialog
import com.devball.jubalaudio.ui.components.dialogs.EditMediaDialog
import com.devball.jubalaudio.ui.components.dialogs.LoadingDialog
import com.devball.jubalaudio.ui.components.dialogs.MediaPicker
import com.devball.jubalaudio.ui.components.dialogs.PlaylistFormDialog
import com.devball.jubalaudio.ui.components.dialogs.PlaylistPicker
import com.devball.jubalaudio.ui.components.listitems.MediaListItemReorderable
import com.devball.jubalaudio.ui.components.listitems.MediaListItemSelectable
import com.devball.jubalaudio.ui.components.listitems.MediaListItemStandard
import com.devball.jubalaudio.ui.components.listitems.StaleUriListItem
import com.devball.jubalaudio.ui.components.optionsheets.MediaOption
import com.devball.jubalaudio.ui.components.optionsheets.MediaOptionsSheetContent
import com.devball.jubalaudio.ui.components.optionsheets.PlaylistOption
import com.devball.jubalaudio.ui.components.optionsheets.PlaylistOptionsSheet
import com.devball.jubalaudio.ui.viewmodels.PlaylistDetailsViewModel
import com.devball.jubalaudio.util.ObserveUiEvents
import com.devball.jubalaudio.util.SwipeDismissable

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
    val playlistWithCount by viewModel.playlistWithCount.collectAsStateWithLifecycle()
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val fullMediaList by viewModel.playlistMedia.collectAsStateWithLifecycle()
    val availablePlaylists by viewModel.availablePlaylists.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    //Show loading dialog if playlist hasn't been fetched yet
    if (playlistWithCount == null) {
        LoadingDialog()
        return
    }

    //Destructure playlistWithCount - not-null assertion because of the if statement above
    val (playlist, itemCount) = playlistWithCount!!

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
                        model = playlist.coverUri,
                        contentDescription = "Cover Image",
                        modifier = Modifier.clickable(onClick = { editingPlaylist = true }),
                        fallbackIcon = Icons.Default.LibraryMusic,
                        sizeInDp = 80.dp
                    )

                    //Playlist Details
                    ItemInfoColumn(
                        paddingValues = PaddingValues(start = 8.dp),
                        line1 = { Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = if (playlist.description != null) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        ) },
                        line2 = { playlist.description?.let { description -> Text(
                            text = description,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) } },
                        line3 = { Text(
                            text = "$itemCount items",
                            style = MaterialTheme.typography.bodyLarge
                        ) }
                    )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(48.dp),
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isReordering = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            ) { Text(text = "Done", style = MaterialTheme.typography.titleMedium) }
                        }
                    }

                    //Selecting = Show Bulk Actions Bar
                    TopBarState.Selecting -> {
                        BulkActionsBar(
                            modifier = Modifier.fillMaxSize(),
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
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            //Search Bar
                            SearchBar(
                                value = searchQuery,
                                placeHolderText = "Search in playlist",
                                modifier = Modifier.weight(1f),
                                onClear = { viewModel.onSearchQueryChange("") },
                                onValueChange = { viewModel.onSearchQueryChange(it) }
                            )

                            //Play Button
                            IconButton(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f),
                                onClick = {
                                    if (activePlaylistId == playlist.playlistId) onTogglePlayPauseClick()
                                    else onPlayPlaylistClick(playlist.playlistId, null)
                                }
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    imageVector =
                                        if (activePlaylistId == playlist.playlistId && isActivePlaylistPlaying) Icons.Default.PauseCircle
                                        else Icons.Default.PlayCircle,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = "Play Playlist"
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
                        EmptyMessage(text = "You have no items in this playlist. Add some using the ellipsis button at the top-right of your screen, " +
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
                            modifier = Modifier.animateItem(),
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
                                        onImageClick = { if(!media.isStaleUri) {
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
    }


    //Show ModalBottomSheet options for this playlist if user clicks the ellipsis at the top right
    if (showPlaylistOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistOptionsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PlaylistOptionsSheet(
                playlist = playlist,
                showReorderOption = true,
                onOptionClick = { option ->
                    showPlaylistOptionsSheet = false
                    when (option) {
                        PlaylistOption.EDIT -> editingPlaylist = true
                        PlaylistOption.PLAY_NOW -> onPlayPlaylistClick(playlist.playlistId, null)
                        PlaylistOption.ADD_TO_QUEUE -> onAddToQueueClick(fullMediaList)
                        PlaylistOption.ADD_MEDIA -> showMediaPicker = true
                        PlaylistOption.REORDER -> isReordering = true
                        PlaylistOption.DELETE -> showDeletePlaylistConfirmation = true
                    }
                }
            )
        }
    }

    //Show PlaylistFormDialog if user wants to edit this playlist
    if (editingPlaylist) {
        PlaylistFormDialog(
            playlistToEdit = playlist,
            onDismiss = { editingPlaylist = false },
            onConfirm = { plist ->
                editingPlaylist = false
                viewModel.editPlaylist(plist)
            }
        )
    }

    //Show ConfirmationDialog if user wants to delete this playlist
    if (showDeletePlaylistConfirmation) {
        ConfirmationDialog(
            title = "Are you sure you want to delete the playlist \"${playlist.name}\"?",
            text = "This action cannot be undone",
            onDismiss = { showDeletePlaylistConfirmation = false },
            onConfirm = {
                onBack()
                viewModel.deletePlaylist(playlist)
            }
        )
    }

    //Show ConfirmationDialog if user wants to remove media items from this playlist
    if (idsToRemove.isNotEmpty()) {
        val text = when {
            idsToRemove.size == 1 -> "\"${mediaList.first { it.mediaId == idsToRemove[0] }.title}\""
            else -> "these ${idsToRemove.size} items"
        }
        ConfirmationDialog(
            title = "Are you sure you want to remove $text from \"${playlist.name}\"?",
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
        MediaPicker(
            media = mediaNotInPlaylist.filter { !it.isStaleUri },
            onDismiss = { showMediaPicker = false },
            onConfirm = { mediaIds ->
                showMediaPicker = false
                viewModel.addMediaToPlaylists(mediaIds, listOf(playlist.playlistId))
            }
        )
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
