package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.common.ItemInfoColumn
import com.devball.jubalaudio.ui.components.common.SelectionIcon
import com.devball.jubalaudio.ui.components.common.SurfacedImage

@Composable
fun MediaListItemSelectable(
    media: MediaEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    constrainSelectToCheckbox: Boolean = true,
    onMoreClick: (MediaEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !constrainSelectToCheckbox,
                onClick = onSelect,
                onLongClick = { onMoreClick(media) }
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Selection Checkbox
        IconButton(onClick = onSelect) {
            SelectionIcon(isSelected)
        }

        //Artwork
        SurfacedImage(
            model = media.artworkUri,
            contentDescription = "Artwork Image"
        )

        //Media Item Info
        ItemInfoColumn(
            line1 = { Text(
                text = media.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            ) },
            line2 = { Text(
                text = media.creator,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            ) }
        )

        //More Button (ellipsis) - brings up menu for edit, play, add to queue, add to playlist, delete, etc.
        IconButton(onClick = { onMoreClick(media) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}