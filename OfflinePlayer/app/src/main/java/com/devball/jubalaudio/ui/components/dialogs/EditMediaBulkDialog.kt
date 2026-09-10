package com.devball.jubalaudio.ui.components.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.ui.components.common.ImagePickerRow

@Composable
fun EditMediaBulkDialog(
    itemCount: Int,
    commonCreator: String = "",
    commonArtwork: String? = null,
    onDismiss: () -> Unit,
    onConfirmCreator: (String) -> Unit,
    onConfirmArtwork: (String?) -> Unit
) {
    var artworkUri by remember { mutableStateOf(commonArtwork) }
    var creator by remember { mutableStateOf(commonCreator) }
    var creatorTouched by remember { mutableStateOf(false) }

    var creatorExpanded by remember { mutableStateOf(false) }
    var artworkExpanded by remember { mutableStateOf(false) }

    //Launcher for picking artwork image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { artworkUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editing $itemCount items") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Fields are prefilled only if the selected items have it in common. " +
                        "Tapping Save WILL update that field for all items with what is shown.",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                //Creator Dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = { creatorExpanded = !creatorExpanded })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = if (artworkExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = if (creatorExpanded) "Collapse" else "Expand",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(text = "Edit Creator", style = MaterialTheme.typography.bodyLarge)
                }

                //Creator Dropdown Content
                AnimatedVisibility(visible = creatorExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = creator,
                            onValueChange = {
                                creator = it
                                creatorTouched = true
                            },
                            isError = creator.isBlank() && creatorTouched,
                            label = { Text("Creator *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            enabled = creator.isNotBlank(),
                            onClick = { onConfirmCreator(creator) },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Save For All") }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )

                //Artwork Dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = { artworkExpanded = !artworkExpanded })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = if (artworkExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = if (artworkExpanded) "Collapse" else "Expand",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(text = "Edit Artwork", style = MaterialTheme.typography.bodyLarge)
                }

                //Artwork Dropdown Content
                AnimatedVisibility(visible = artworkExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        ImagePickerRow(
                            model = artworkUri,
                            contentDescription = "Artwork Image (Bulk)",
                            mainText = "Artwork",
                            onImageClick = { pickImageLauncher.launch("image/*") },
                            onRemoveClick = { artworkUri = null }
                        )
                        Button(
                            onClick = { onConfirmArtwork(artworkUri) },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Save For All") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Exit")
            }
        }
    )
}