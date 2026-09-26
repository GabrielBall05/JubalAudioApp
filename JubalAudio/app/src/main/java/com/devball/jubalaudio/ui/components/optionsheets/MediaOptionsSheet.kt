package com.devball.jubalaudio.ui.components.optionsheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity

enum class MediaOption(
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false
) {
    EDIT(
        label = "Edit Details",
        icon = Icons.Default.Edit
    ),
    PLAY_NOW(
        label = "Play",
        icon = Icons.Default.PlayArrow
    ),
    ADD_TO_QUEUE(
        label = "Add to Queue",
        icon = Icons.Default.AddToQueue
    ),
    ADD_TO_PLAYLIST(
        label = "Add to Playlist",
        icon = Icons.AutoMirrored.Filled.PlaylistAdd
    ),
    REMOVE_FROM_PLAYLIST(
        label = "Remove from Playlist",
        icon = Icons.Default.PlaylistRemove,
        isDestructive = true
    ),
    DELETE(
        label = "Delete",
        icon = Icons.Default.DeleteForever,
        isDestructive = true
    )
}

data class MediaSheetAction(
    val option: MediaOption,
    val onClick: () -> Unit
)

@Composable
fun MediaOptionsSheet(
    title: String,
    actions: List<MediaSheetAction>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            maxLines = 1
        )

        HorizontalDivider()

        actions.forEach { (option, onClick) ->
            MenuOptionItem(
                icon = option.icon,
                label = option.label,
                isDestructive = option.isDestructive,
                onClick = {
                    onDismiss() //Always dismiss the sheet
                    onClick() //Execute option click logic
                }
            )
        }
    }
}