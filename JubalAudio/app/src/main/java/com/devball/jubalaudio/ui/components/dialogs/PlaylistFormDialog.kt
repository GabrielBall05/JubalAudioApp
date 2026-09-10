package com.devball.jubalaudio.ui.components.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.PlaylistEntity
import com.devball.jubalaudio.ui.components.common.ImagePickerRow

@Composable
fun PlaylistFormDialog(
    playlistToEdit: PlaylistEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (PlaylistEntity) -> Unit
) {
    var name by remember { mutableStateOf(playlistToEdit?.name ?: "") }
    var description by remember { mutableStateOf(playlistToEdit?.description ?: "") }
    var coverUri by remember { mutableStateOf(playlistToEdit?.coverUri) }

    var nameTouched by remember { mutableStateOf(false) }

    //Launcher for picking cover image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { coverUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlistToEdit?.let { "Editing Playlist: \"${playlistToEdit.name}\"" } ?: "Create Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                //Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameTouched = true
                    },
                    label = { Text("Name *") },
                    isError = nameTouched && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                //Description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                //Cover Image
                ImagePickerRow(
                    model = coverUri,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    contentDescription = "Cover Image",
                    mainText = "Cover Image",
                    onImageClick = { pickImageLauncher.launch("image/*") },
                    onRemoveClick = { coverUri = null }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDescription = description.takeIf { it.isNotBlank() }
                    //val finalCoverUri = coverUri.takeIf { it.isNotBlank() }

                    playlistToEdit?.let {
                        onConfirm(it.copy(name = name, description = finalDescription, coverUri = coverUri))
                    } ?: run {
                        val newPlaylist = PlaylistEntity(playlistId = 0, name = name, description = finalDescription, coverUri = coverUri, dateCreated = System.currentTimeMillis())
                        onConfirm(newPlaylist)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(playlistToEdit?.let { "Save" } ?: "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}