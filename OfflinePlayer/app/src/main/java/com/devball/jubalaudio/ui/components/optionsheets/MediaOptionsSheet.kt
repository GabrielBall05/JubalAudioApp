package com.devball.jubalaudio.ui.components.optionsheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity

enum class MediaOption {
    EDIT, PLAY_NOW, ADD_TO_QUEUE, ADD_TO_PLAYLIST, REMOVE_FROM_PLAYLIST, DELETE
}

@Composable
fun MediaOptionsSheetContent(
    media: MediaEntity,
    showEditOption: Boolean = true,
    showPlayOption: Boolean = true,
    showRemoveOption: Boolean = false,
    showDeleteOption: Boolean = false,
    onOptionClick: (MediaOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = media.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            maxLines = 1
        )

        HorizontalDivider()

        //Edit
        if (showEditOption) {
            MenuOptionItem(
                icon = Icons.Default.Edit,
                label = "Edit Details",
                onClick = { onOptionClick(MediaOption.EDIT) }
            )
        }
        //Play now
        if (showPlayOption) {
            MenuOptionItem(
                icon = Icons.Default.PlayArrow,
                label = "Play",
                onClick = { onOptionClick(MediaOption.PLAY_NOW) }
            )
        }
        //Add to Queue
        MenuOptionItem(
            icon = Icons.Default.AddToQueue,
            label = "Add to Queue",
            onClick = { onOptionClick(MediaOption.ADD_TO_QUEUE) }
        )
        //Add to Playlist
        MenuOptionItem(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            label = "Add to Playlist",
            onClick = { onOptionClick(MediaOption.ADD_TO_PLAYLIST) }
        )
        //Remove from Playlist
        if (showRemoveOption) {
            MenuOptionItem(
                icon = Icons.Default.PlaylistRemove,
                label = "Remove from Playlist",
                isDestructive = true,
                onClick = { onOptionClick(MediaOption.REMOVE_FROM_PLAYLIST) }
            )
        }
        //Delete
        if (showDeleteOption) {
            MenuOptionItem(
                icon = Icons.Default.DeleteForever,
                label = "Delete",
                isDestructive = true,
                onClick = { onOptionClick(MediaOption.DELETE) }
            )
        }
    }
}