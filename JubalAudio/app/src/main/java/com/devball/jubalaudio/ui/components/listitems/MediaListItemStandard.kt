package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.common.InfoColumnMarquee
import com.devball.jubalaudio.ui.components.common.SurfacedImage

@Composable
fun MediaListItemStandard(
    media: MediaEntity,
    onImageClick: () -> Unit,
    onLongClick: (MediaEntity) -> Unit,
    onMoreClick: (MediaEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onImageClick,
                onLongClick = { onLongClick(media) }
            )
            .padding(start = 12.dp, top = 2.dp, end = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Artwork
        SurfacedImage(
            model = media.artworkUri,
            contentDescription = "Artwork Image"
        )

        //Media Item Info
        InfoColumnMarquee(
            mainText = media.title,
            subText = media.creator
        )

        //More Button (ellipsis) - brings up menu for edit, play, add to queue, add to playlist, delete, etc.
        IconButton(onClick = { onMoreClick(media) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}