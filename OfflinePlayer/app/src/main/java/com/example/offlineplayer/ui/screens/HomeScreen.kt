package com.example.offlineplayer.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.ui.components.common.BulkActionsBar
import com.example.offlineplayer.ui.components.common.EmptyMessage
import com.example.offlineplayer.ui.components.common.SearchBar
import com.example.offlineplayer.ui.components.dialogs.ConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaBulkDialog
import com.example.offlineplayer.ui.components.dialogs.EditMediaDialog
import com.example.offlineplayer.ui.components.dialogs.LoadingDialog
import com.example.offlineplayer.ui.components.dialogs.PlaylistFormDialog
import com.example.offlineplayer.ui.components.dialogs.PlaylistPicker
import com.example.offlineplayer.ui.components.dialogs.SortOrderDialog
import com.example.offlineplayer.ui.components.listitems.MediaListItemSelectable
import com.example.offlineplayer.ui.components.listitems.StaleUriListItem
import com.example.offlineplayer.ui.components.optionsheets.MediaOption
import com.example.offlineplayer.ui.components.optionsheets.MediaOptionsSheetContent
import com.example.offlineplayer.ui.viewmodels.HomeViewModel
import com.example.offlineplayer.util.MediaSortOrder
import com.example.offlineplayer.util.ObserveUiEvents
import com.example.offlineplayer.util.UiEvent
import com.example.offlineplayer.util.indicatorBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(), //Let Hilt inject the ViewModel
    onPlayMediaClick: (MediaEntity) -> Unit,
    onAddToQueueClick: (List<MediaEntity>) -> Unit
) {
    //Ui Event Observer
    ObserveUiEvents(eventFlow = viewModel.uiEvent)

    //Collect states from ViewModel
    val hasMedia by viewModel.hasMedia.collectAsStateWithLifecycle()
    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isAnySelected by viewModel.isAnySelected.collectAsStateWithLifecycle()
    val isAllSelected by viewModel.isAllSelected.collectAsStateWithLifecycle()
    val availablePlaylists by viewModel.availablePlaylists.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val allStaleMedia by viewModel.allStaleMedia.collectAsStateWithLifecycle()
    val staleListForDisplay by remember { derivedStateOf { mediaList.filter { it.isStaleUri } } }

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()
    val mediaMap = remember(mediaList) { mediaList.associateBy { it.mediaId } }

    var idsToDelete by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var idsToAddToPlaylists by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var selectedMediaItemForMenu by rememberSaveable { mutableStateOf<MediaEntity?>(null) }
    var mediaToEdit by rememberSaveable { mutableStateOf<MediaEntity?>(null) }
    var idsToEdit by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var mediaIdToRelink by rememberSaveable { mutableStateOf<Int?>(null) }
    var showOnlyStale by rememberSaveable { mutableStateOf(false) }

    //Bulk File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.importMedia(uris)
    }

    //Single File Picker Launcher
    val relinkLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { newUri -> mediaIdToRelink?.let { id ->
            viewModel.relinkMedia(id, newUri)
        } }
        mediaIdToRelink = null
    }

    //Jump to top of list when list size changes, sort order is changed, or stale list only toggled
    LaunchedEffect(mediaList.size, sortOrder, showOnlyStale) {
        if (mediaList.isNotEmpty()) listState.scrollToItem(0)
    }

    //Refresh available playlists when the selection for playlist addition is set
    LaunchedEffect(idsToAddToPlaylists, availablePlaylists.size) {
        if (idsToAddToPlaylists.isNotEmpty()) viewModel.refreshAvailablePlaylists(idsToAddToPlaylists)
    }

    //Reset stale filter if all stale items are resolved
    LaunchedEffect(allStaleMedia.isEmpty()) {
        if (allStaleMedia.isEmpty() && showOnlyStale) showOnlyStale = false
    }


    //Screen UI
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            //Page Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Add or Edit Media",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            //Search + Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Search Bar
                SearchBar(
                    value = searchQuery,
                    placeHolderText = "Search media...",
                    modifier = Modifier.weight(1f),
                    onClear = { viewModel.onSearchQueryChange("") },
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                //Show Stale Media Button
                if (allStaleMedia.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        modifier = Modifier
                            .indicatorBorder(enabled = showOnlyStale, color = MaterialTheme.colorScheme.error)
                            .clip(CircleShape),
                        onClick = { showOnlyStale = !showOnlyStale }
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Default.Error,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = "Show Invalid Media"
                        )
                    }
                }

                //Sort
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.AutoMirrored.Default.Sort, contentDescription = "Sort List")
                }
            }

            if (!showOnlyStale) {
                //Bulk Actions
                BulkActionsBar(
                    isAnySelected = isAnySelected,
                    isAllSelected = isAllSelected,
                    onToggleAllClick = { viewModel.toggleSelectAll() },
                    onClearSelectionClick = { viewModel.clearSelection() }
                ) {
                    IconButton(onClick = { idsToEdit = selectedIds.toList() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { idsToAddToPlaylists = selectedIds.toList() }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add To Playlist")
                    }
                    IconButton(onClick = { onAddToQueueClick(selectedIds.mapNotNull { id -> mediaMap[id] }) }) {
                        Icon(Icons.Default.AddToQueue, contentDescription = "Add Selection to Queue")
                    }
                    IconButton(onClick = { idsToDelete = selectedIds.toList() }) {
                        Icon(Icons.Default.DeleteForever, tint = MaterialTheme.colorScheme.error, contentDescription = "Delete")
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(start = 16.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.Absolute.Left,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        text = "The following have invalid file paths.\nFix using relink buttons or delete."
                    )
                    TextButton(
                        onClick = { idsToDelete = allStaleMedia.map { it.mediaId } },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete ALL")
                    }
                }
            }

            //Media List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (!hasMedia) {
                    item {
                        EmptyMessage(text = "You have no items uploaded. Add some using the \"+\" button at the bottom-right of your screen.")
                    }
                } else {
                    val activeList = if (showOnlyStale) staleListForDisplay else mediaList

                    if (activeList.isEmpty()) {
                        item {
                            EmptyMessage(text = if (showOnlyStale) "No invalid media match your search." else "No matches found.")
                        }
                    } else {
                        items(
                            items = activeList,
                            key = { it.mediaId }
                        ) { media ->
                            //Show selectable list item under normal circumstances
                            if (!media.isStaleUri) {
                                MediaListItemSelectable(
                                    media = media,
                                    isSelected = selectedIds.contains(media.mediaId),
                                    onSelect = { viewModel.toggleSelection(media.mediaId) },
                                    constrainSelectToCheckbox = false,
                                    onMoreClick = { selectedMediaItemForMenu = media }
                                )
                            } else { //Show error list item when uri is stale
                                StaleUriListItem(
                                    media = media,
                                    onRelinkClick = {
                                        mediaIdToRelink = media.mediaId
                                        relinkLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onDeleteClick = { idsToDelete = listOf(media.mediaId) }
                                )
                            }
                        }
                    }
                }
//                if (!hasMedia) {
//                    item {
//                        EmptyMessage(text = "You have no items uploaded. Add some using the \"+\" button at the bottom-right of your screen.")
//                    }
//                } else if (mediaList.isEmpty()) {
//                    item {
//                        EmptyMessage(text = "No matches found.")
//                    }
//                } else {
//                    if (showOnlyStale) {
//                        items(
//                            items = staleList,
//                            key = { it.mediaId }
//                        ) { media ->
//                            StaleUriListItem(
//                                media = media,
//                                onRelinkClick = {
//                                    mediaIdToRelink = media.mediaId
//                                    relinkLauncher.launch(arrayOf("audio/*"))
//                                },
//                                onDeleteClick = { idsToDelete = listOf(media.mediaId) }
//                            )
//                        }
//                    } else {
//                        items(
//                            items = mediaList,
//                            key = { it.mediaId }
//                        ) { media ->
//                            //Show selectable list item under normal circumstances
//                            if (!media.isStaleUri) {
//                                MediaListItemSelectable(
//                                    media = media,
//                                    isSelected = selectedIds.contains(media.mediaId),
//                                    onSelect = { viewModel.toggleSelection(media.mediaId) },
//                                    constrainSelectToCheckbox = false,
//                                    onMoreClick = { selectedMediaItemForMenu = media }
//                                )
//                            } else { //Show error list item when uri is stale
//                                StaleUriListItem(
//                                    media = media,
//                                    onRelinkClick = {
//                                        mediaIdToRelink = media.mediaId
//                                        relinkLauncher.launch(arrayOf("audio/*"))
//                                    },
//                                    onDeleteClick = { idsToDelete = listOf(media.mediaId) }
//                                )
//                            }
//                        }
//                    }
//                }
            }
        }

        //Upload Media Button (FAB)
        FloatingActionButton(
            onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload Media")
        }
    }


    //Show options menu if user hits ellipsis on a media item
    selectedMediaItemForMenu?.let { media ->
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItemForMenu = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MediaOptionsSheetContent(
                media = media,
                showDeleteOption = true,
                onOptionClick = { option ->
                    selectedMediaItemForMenu = null
                    when (option) {
                        MediaOption.EDIT -> mediaToEdit = media
                        MediaOption.PLAY_NOW -> onPlayMediaClick(media)
                        MediaOption.ADD_TO_QUEUE -> onAddToQueueClick(listOf(media))
                        MediaOption.ADD_TO_PLAYLIST -> idsToAddToPlaylists = listOf(media.mediaId)
                        MediaOption.REMOVE_FROM_PLAYLIST -> { /* Not used in home screen */ }
                        MediaOption.DELETE -> idsToDelete = listOf(media.mediaId)
                    }
                }
            )
        }
    }

    //Show edit dialog if user hit edit (single)
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

    //Show PlaylistPicker if user clicks Add to Playlist (bulk or single)
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

    //Show SortOrderDialog if user clicks Sort button
    if (showSortDialog) {
        SortOrderDialog(
            title = "Sort Media By",
            options = MediaSortOrder.entries.toTypedArray(),
            currentSelection = sortOrder,
            onDismiss = { showSortDialog = false },
            onOptionSelected = { option ->
                showSortDialog = false
                viewModel.onSortOrderChange(option)
            }
        )
    }

    //Show delete confirmation dialog if user hit delete
    if (idsToDelete.isNotEmpty()) {
        val text = when {
            idsToDelete.size == 1 -> "\"${mediaList.first { it.mediaId == idsToDelete[0] }.title}\""
            else -> "these ${idsToDelete.size} items"
        }
        ConfirmationDialog(
            title = "Are you sure you want to delete $text from your library?",
            text = "This action cannot be undone",
            onDismiss = { idsToDelete = emptyList() },
            onConfirm = {
                viewModel.deleteMediaByIds(idsToDelete)
                idsToDelete = emptyList()
                viewModel.clearSelection()
            }
        )
    }

    //Show loading screen for potentially long operations
    if (isLoading) {
        LoadingDialog()
    }
}
