package com.devball.jubalaudio.ui.components.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.common.ImagePickerRow

@Composable
fun EditMediaDialog(
    media: MediaEntity,
    onDismiss: () -> Unit,
    onConfirm: (MediaEntity) -> Unit
) {
    var title by remember { mutableStateOf(media.title) }
    var creator by remember { mutableStateOf(media.creator) }
    var artworkUri by remember { mutableStateOf(media.artworkUri) }

    var titleTouched by remember { mutableStateOf(false) }
    var creatorTouched by remember { mutableStateOf(false) }

    //Launcher for picking artwork image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { artworkUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Display Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                //Edit title input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleTouched = true
                    },
                    isError = titleTouched && title.isBlank(),
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                //Edit creator input
                OutlinedTextField(
                    value = creator,
                    onValueChange = {
                        creator = it
                        creatorTouched = true
                    },
                    isError = creatorTouched && creator.isBlank(),
                    label = { Text("Creator *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                //Artwork Image (Shows the SurfacedImage as well as info + remove option)
                ImagePickerRow(
                    model = artworkUri,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    contentDescription = "Artwork Image",
                    onImageClick = { pickImageLauncher.launch("image/*") },
                    onRemoveClick = { artworkUri = null }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(media.copy(
                        title = title,
                        creator = creator,
                        artworkUri = artworkUri
                    ))
                },
                enabled = title.isNotBlank() && creator.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}