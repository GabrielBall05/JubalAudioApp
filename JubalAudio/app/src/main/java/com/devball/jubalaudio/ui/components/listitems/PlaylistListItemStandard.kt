package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.PlaylistEntity
import com.devball.jubalaudio.ui.components.common.ItemInfoColumn
import com.devball.jubalaudio.ui.components.common.SurfacedImage

@Composable
fun PlaylistListItemStandard(
    playlist: PlaylistEntity,
    countText: String,
    modifier: Modifier = Modifier,
    onMoreClick: (PlaylistEntity) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
       verticalAlignment = Alignment.CenterVertically
    ) {
        //Artwork
        SurfacedImage(
            model = playlist.coverUri,
            contentDescription = "Cover Image",
            fallbackIcon = Icons.Default.LibraryMusic,
            sizeInDp = 65.dp
        )


        //Playlist Item Info
        ItemInfoColumn(
            line1 = { Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            ) },
            line2 = { Text(
                text = countText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            ) }
        )

        //More Button - Brings up menu for things like delete
        IconButton(onClick = { onMoreClick(playlist) }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
        }
    }
}