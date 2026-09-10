package com.devball.jubalaudio.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.PlaylistEntity
import com.devball.jubalaudio.ui.components.listitems.PlaylistPickerListItem

@Composable
fun PlaylistPicker(
    playlists: List<PlaylistEntity>,
    onCreateClick: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val selectedPlaylistIds = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Playlist(s)") },
        text = {
            if (playlists.isEmpty()) {
                Text("No playlists exist or all selected items are present in every playlist.")
            }
            else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = playlists,
                        key = { it.playlistId }
                    ) { playlist ->
                        PlaylistPickerListItem(
                            playlist = playlist,
                            isSelected = selectedPlaylistIds.contains(playlist.playlistId),
                            onClick = {
                                if (selectedPlaylistIds.contains(playlist.playlistId)) selectedPlaylistIds.remove(playlist.playlistId)
                                else selectedPlaylistIds.add(playlist.playlistId)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCreateClick) {
                    Text("Create")
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                Button(
                    enabled = selectedPlaylistIds.isNotEmpty(),
                    onClick = { onConfirm(selectedPlaylistIds.toList()) }
                ) {
                    Text("Add")
                }
            }
        },
        dismissButton = null
    )
}