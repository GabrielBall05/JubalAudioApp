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
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.PlaylistEntity

enum class PlaylistOption {
    EDIT, PLAY_NOW, ADD_TO_QUEUE, REORDER, ADD_MEDIA, DELETE
}

@Composable
fun PlaylistOptionsSheet(
    playlist: PlaylistEntity,
    showReorderOption: Boolean = false,
    onOptionClick: (PlaylistOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            maxLines = 1
        )

        HorizontalDivider()

        //Edit
        MenuOptionItem(
            icon = Icons.Default.Edit,
            label = "Edit",
            onClick = { onOptionClick(PlaylistOption.EDIT) }
        )

        //Play Now
        MenuOptionItem(
            icon = Icons.Default.PlayArrow,
            label = "Play Now",
            onClick = { onOptionClick(PlaylistOption.PLAY_NOW) }
        )

        //Add to Queue
        MenuOptionItem(
            icon = Icons.Default.AddToQueue,
            label = "Add Playlist to Queue",
            onClick = { onOptionClick(PlaylistOption.ADD_TO_QUEUE) }
        )

        //Add Media
        MenuOptionItem(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            label = "Add Media",
            onClick = { onOptionClick(PlaylistOption.ADD_MEDIA) }
        )

        //Reorder
        if (showReorderOption) {
            MenuOptionItem(
                icon = Icons.Default.Reorder,
                label = "Reorder Playlist Items",
                onClick = { onOptionClick(PlaylistOption.REORDER) }
            )
        }

        //Delete
        MenuOptionItem(
            icon = Icons.Default.DeleteForever,
            label = "Delete",
            isDestructive = true,
            onClick = { onOptionClick(PlaylistOption.DELETE) }
        )
    }
}