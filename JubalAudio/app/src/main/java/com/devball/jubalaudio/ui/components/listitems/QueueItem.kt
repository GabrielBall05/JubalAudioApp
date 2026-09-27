package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.devball.jubalaudio.ui.components.common.SurfacedImage
import com.devball.jubalaudio.ui.components.common.WeightedColumn

val QueueItemPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 24.dp, bottom = 4.dp)
val QueueItemImageSize = 50.dp

@Composable
fun QueueItem(
    item: MediaItem,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {  }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(QueueItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Artwork
        SurfacedImage(
            modifier = Modifier.clickable(
                enabled = !isPlaying,
                onClick = onClick
            ),
            model = item.mediaMetadata.artworkUri.toString(),
            contentDescription = "Queue Item Artwork",
            sizeInDp = QueueItemImageSize
        )

        //Queue (Media) Item Info
        WeightedColumn {
            Text(
                text = item.mediaMetadata.title.toString(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.mediaMetadata.artist.toString(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        //Reordering Drag Handle
        if (!isPlaying) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                modifier = dragHandleModifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}