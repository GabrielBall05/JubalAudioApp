package com.devball.jubalaudio.ui.components.listitems

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.common.ItemInfoColumn
import com.devball.jubalaudio.ui.components.common.SurfacedImage

@Composable
fun MediaListItemReorderable(
    modifier: Modifier = Modifier,
    media: MediaEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp, end = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                overflow = TextOverflow.Ellipsis
            ) },
            line2 = { Text(
                text = media.creator,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            ) }
        )

        //Reorder Buttons
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