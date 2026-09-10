package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.devball.jubalaudio.ui.components.common.InfoColumnMarquee
import com.devball.jubalaudio.ui.components.common.SurfacedImage

val QueueItemPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
val QueueItemImageSize = 50.dp

@Composable
fun QueueItem(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isFirst: Boolean,
    isLast: Boolean,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {  },
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
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
        InfoColumnMarquee(
            mainText = item.mediaMetadata.title.toString(),
            mainTextStyle = MaterialTheme.typography.titleMedium,
            subText = item.mediaMetadata.artist.toString(),
            subTextStyle = MaterialTheme.typography.bodyMedium
        )

        //Reordering Buttons
        if (!(isFirst && isLast)) {
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
            }
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
            }
        }
    }
}