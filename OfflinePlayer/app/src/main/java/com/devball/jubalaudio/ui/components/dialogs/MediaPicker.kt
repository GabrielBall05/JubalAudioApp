package com.devball.jubalaudio.ui.components.dialogs

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.listitems.MediaPickerListItem

@Composable
fun MediaPicker(
    media: List<MediaEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val selectedMediaIds = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Media") },
        text = {
            if (media.isEmpty()) Text("No media to add. Either this playlist contains all your media, or you have none and need to upload media from the home page.")
            else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = media,
                        key = { it.mediaId }
                    ) { item ->
                        MediaPickerListItem(
                            media = item,
                            isSelected = selectedMediaIds.contains(item.mediaId),
                            onClick = {
                                if (selectedMediaIds.contains(item.mediaId)) selectedMediaIds.remove(item.mediaId)
                                else selectedMediaIds.add(item.mediaId)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedMediaIds.isNotEmpty(),
                onClick = { onConfirm(selectedMediaIds.toList()) }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}