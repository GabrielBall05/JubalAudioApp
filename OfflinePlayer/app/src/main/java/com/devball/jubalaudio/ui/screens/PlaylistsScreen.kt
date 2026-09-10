package com.devball.jubalaudio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.data.local.PlaylistEntity
import com.devball.jubalaudio.ui.Screen
import com.devball.jubalaudio.ui.components.common.EmptyMessage
import com.devball.jubalaudio.ui.components.common.SearchBar
import com.devball.jubalaudio.ui.components.dialogs.ConfirmationDialog
import com.devball.jubalaudio.ui.components.dialogs.LoadingDialog
import com.devball.jubalaudio.ui.components.dialogs.MediaPicker
import com.devball.jubalaudio.ui.components.dialogs.PlaylistFormDialog
import com.devball.jubalaudio.ui.components.dialogs.SortOrderDialog
import com.devball.jubalaudio.ui.components.listitems.PlaylistListItemStandard
import com.devball.jubalaudio.ui.components.optionsheets.PlaylistOption
import com.devball.jubalaudio.ui.components.optionsheets.PlaylistOptionsSheet
import com.devball.jubalaudio.ui.viewmodels.PlaylistsViewModel
import com.devball.jubalaudio.util.ObserveUiEvents
import com.devball.jubalaudio.util.PlaylistsSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    navController: NavController,
    viewModel: PlaylistsViewModel = hiltViewModel(), //Let Hilt inject the ViewModel
    onPlayPlaylistClick: (Int) -> Unit,
    onAddPlaylistToQueueClick: (Int) -> Unit
) {
    //Ui Event Observer
    ObserveUiEvents(eventFlow = viewModel.uiEvent)

    //Collect states from ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playlistList by viewModel.filteredPlaylists.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var playlistToAddMedia by rememberSaveable { mutableStateOf<PlaylistEntity?>(null) }
    var mediaNotInPlaylist by rememberSaveable { mutableStateOf<List<MediaEntity>>(emptyList()) }
    var isFetchingMedia by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylistForMenu by rememberSaveable { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToEdit by rememberSaveable { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by rememberSaveable { mutableStateOf<PlaylistEntity?>(null) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    //Jump to top of list when list size changes or sort order is changed
    LaunchedEffect(playlistList.size, sortOrder) {
        if (playlistList.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    //Fetch media not in playlist when picker is shown for playlistToAddMedia
    LaunchedEffect(playlistToAddMedia) {
        playlistToAddMedia?.let { playlist ->
            isFetchingMedia = true
            mediaNotInPlaylist = viewModel.getMediaNotInPlaylist(playlist.playlistId)
            isFetchingMedia = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            //Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Add or Edit Playlists",
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
                //Search
                SearchBar(
                    value = searchQuery,
                    placeHolderText = "Search playlists...",
                    modifier = Modifier.weight(1f),
                    onClear = { viewModel.onSearchQueryChange("") },
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                //Sort
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.AutoMirrored.Default.Sort, contentDescription = "Sort List")
                }
            }

            //Playlist List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (playlistList.isNotEmpty()) {
                    items(
                        items = playlistList,
                        key = { it.playlistId }
                    ) { playlist ->
                        PlaylistListItemStandard(
                            playlist = playlist,
                            modifier = Modifier.clickable {
                                navController.navigate(Screen.PlaylistDetails.createRoute(playlist.playlistId))
                            },
                            onMoreClick = { selectedPlaylistForMenu = playlist }
                        )
                    }
                } else {
                    item {
                        EmptyMessage(text = "You have no playlists made. Create one using the \"+\" button at the bottom-right of your screen.")
                    }
                }
            }
        }

        //Create Playlist Button
        FloatingActionButton(
            onClick = { creatingPlaylist = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Playlist")
        }
    }

    //Show MediaPicker if user clicks Add Media
    playlistToAddMedia?.let { currentPlaylist ->
        MediaPicker(
            media = mediaNotInPlaylist.filter { !it.isStaleUri },
            onDismiss = { playlistToAddMedia = null },
            onConfirm = { mediaIds ->
                viewModel.addMediaToPlaylists(mediaIds, listOf(currentPlaylist.playlistId))
                playlistToAddMedia = null
            }
        )
    }

    //Show PlaylistFormDialog if user clicks FAB
    if (creatingPlaylist) {
        PlaylistFormDialog(
            onDismiss = { creatingPlaylist = false },
            onConfirm = { playlist ->
                viewModel.createPlaylist(playlist)
                creatingPlaylist = false
            }
        )
    }

    //Show options menu if user hits ellipses on playlist
    selectedPlaylistForMenu?.let { playlist ->
        ModalBottomSheet(
            onDismissRequest = { selectedPlaylistForMenu = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PlaylistOptionsSheet(
                playlist = playlist,
                onOptionClick = { option ->
                    selectedPlaylistForMenu = null
                    when (option) {
                        PlaylistOption.EDIT -> playlistToEdit = playlist
                        PlaylistOption.PLAY_NOW -> onPlayPlaylistClick(playlist.playlistId)
                        PlaylistOption.ADD_TO_QUEUE -> onAddPlaylistToQueueClick(playlist.playlistId)
                        PlaylistOption.REORDER -> { /* Reorder option not used here */ }
                        PlaylistOption.ADD_MEDIA -> playlistToAddMedia = playlist
                        PlaylistOption.DELETE -> playlistToDelete = playlist
                    }
                }
            )
        }
    }

    //Show PlaylistFormDialog if user clicks Edit
    playlistToEdit?.let {
        PlaylistFormDialog(
            playlistToEdit = playlistToEdit,
            onDismiss = { playlistToEdit = null },
            onConfirm = { playlist ->
                playlistToEdit = null
                viewModel.editPlaylist(playlist)
            }
        )
    }

    //Show SortOrderDialog if user clicks Sort button
    if (showSortDialog) {
        SortOrderDialog(
            title = "Sort Playlists By",
            options = PlaylistsSortOrder.entries.toTypedArray(),
            currentSelection = sortOrder,
            onDismiss = { showSortDialog = false },
            onOptionSelected = { option ->
                showSortDialog = false
                viewModel.onSortOrderChange(option)
            }
        )
    }

    //Show delete dialog if user hits delete
    playlistToDelete?.let { playlist ->
        ConfirmationDialog(
            title = "Are you sure you want to delete playlist: \"${playlist.name}\"?",
            text = "This action cannot be undone",
            onConfirm = {
                viewModel.deletePlaylist(playlist)
                playlistToDelete = null
            },
            onDismiss =  {
                playlistToDelete = null
            }
        )
    }

    //Show loading screen for potentially long operations
    if (isLoading) {
        LoadingDialog()
    }
}
