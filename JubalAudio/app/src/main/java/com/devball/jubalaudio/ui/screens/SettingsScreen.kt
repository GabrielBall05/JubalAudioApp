package com.devball.jubalaudio.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devball.jubalaudio.ui.components.dialogs.ConfirmationDialog
import com.devball.jubalaudio.ui.components.listitems.SettingsItem
import com.devball.jubalaudio.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(), //Let Hilt inject the ViewModel
    onClearPlayer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
    ) {
        //Clear Player
        SettingsItem(
            mainText = "Clear Player State",
            subText = "This will completely clear the state of the player. Any media currently playing will be wiped for a clean slate."
        ) {
            Button(
                onClick = { showClearConfirmation = true },
            ) { Text("Clear") }
        }

        //Keep Screen On
        SettingsItem(
            mainText = "Keep Screen On In Player Screen",
            subText = "If enabled, the screen will not timeout while the full player screen is open."
        ) {
            Switch(
                checked = uiState.keepScreenOn,
                onCheckedChange = { viewModel.setKeepScreenOn(it) }
            )
        }

        //Infinite Playback
        SettingsItem(
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