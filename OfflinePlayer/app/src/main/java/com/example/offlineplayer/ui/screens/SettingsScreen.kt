package com.example.offlineplayer.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.offlineplayer.ui.components.dialogs.ConfirmationDialog
import com.example.offlineplayer.ui.components.dialogs.SortOrderDialog
import com.example.offlineplayer.ui.components.listitems.settingsItem
import com.example.offlineplayer.ui.viewmodels.SettingsViewModel
import com.example.offlineplayer.util.MediaSortOrder
import com.example.offlineplayer.util.PlaylistsSortOrder

enum class ActiveDialogType {
    MEDIA_SORT,
    PLAYLIST_SORT
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(), //Let Hilt inject the ViewModel
    onClearPlayer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var activeDialog by rememberSaveable { mutableStateOf<ActiveDialogType?>(null) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
    ) {
        //Clear Player
        settingsItem(
            mainText = "Clear Player State",
            subText = "This will completely clear the state of the player. Any media currently playing will be wiped for a clean slate."
        ) {
            Button(
                onClick = { showClearConfirmation = true },
            ) { Text("Clear") }
        }

        //Keep Screen On
        settingsItem(
            mainText = "Keep Screen On In Player Screen",
            subText = "If enabled, the screen will not timeout while the full player screen is open."
        ) {
            Switch(
                checked = uiState.keepScreenOn,
                onCheckedChange = { viewModel.setKeepScreenOn(it) }
            )
        }

        //Default Media Sort Order
        settingsItem(
            mainText = "Default Media Sort Order",
            subText = "Set the default sorting order of your media list in the Home screen." +
                    "\nCurrent: ${uiState.defaultMediaSortOrder.label}"
        ) {
            IconButton(
                onClick = { activeDialog = ActiveDialogType.MEDIA_SORT },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Change Default Media Sort Order",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        //Default Playlists Sort Order
        settingsItem(
            mainText = "Default Playlists Sort Order",
            subText = "Set the default sorting order of your playlists list in the Playlists screen." +
                    "\nCurrent: ${uiState.defaultPlaylistsSortOrder.label}"
        ) {
            IconButton(
                onClick = { activeDialog = ActiveDialogType.PLAYLIST_SORT },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Change Default Playlists Sort Order",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        //Infinite Playback
        settingsItem(
            mainText = "Infinite Playback",
            subText = "If enabled, when the player reaches the end of the timeline, your full playlist will repeat." +
                    "\nNote: This only applies when there is an active playlist."
        ) {
            Switch(
                checked = uiState.infinitePlayback,
                onCheckedChange = { viewModel.setInfinitePlayback(it) }
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }

    //Show appropriate sorting dialog
    when (activeDialog) {
        ActiveDialogType.MEDIA_SORT -> {
            SortOrderDialog(
                title = "Sort Media By",
                options = MediaSortOrder.entries.toTypedArray(),
                currentSelection = uiState.defaultMediaSortOrder,
                onDismiss = { activeDialog = null },
                onOptionSelected = { option ->
                    activeDialog = null
                    viewModel.setDefaultMediaSortOrder(option)
                }
            )
        }
        ActiveDialogType.PLAYLIST_SORT -> {
            SortOrderDialog(
                title = "Sort Playlists By",
                options = PlaylistsSortOrder.entries.toTypedArray(),
                currentSelection = uiState.defaultPlaylistsSortOrder,
                onDismiss = { activeDialog = null },
                onOptionSelected = { option ->
                    activeDialog = null
                    viewModel.setDefaultPlaylistsSortOrder(option)
                }
            )
        }
        null -> { /* Do nothing */ }
    }

    //Show confirmation if user hits clear player
    if (showClearConfirmation) {
        ConfirmationDialog(
            title = "Are you sure?",
            text = "Clearing the player will wipe all audio tracks from the player's timeline, giving you a fresh slate to start playing audio.",
            confirmText = "Yes",
            onDismiss = { showClearConfirmation = false },
            onConfirm = {
                showClearConfirmation = false
                onClearPlayer()
            }
        )
    }
}