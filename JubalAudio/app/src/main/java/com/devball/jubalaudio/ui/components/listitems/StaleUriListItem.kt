package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.common.IconPulseEffect
import com.devball.jubalaudio.ui.components.common.IconWithTooltip
import com.devball.jubalaudio.ui.components.common.ItemInfoColumn
import com.devball.jubalaudio.ui.components.common.SurfacedImage

@Composable
fun StaleUriListItem(
    media: MediaEntity,
    onRelinkClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWithTooltip(
            tooltipText = "This item has an invalid file path. Click the relink button to link a new file path.",
            pulseEffect = IconPulseEffect.Scaling
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = "Error: Invalid File Path"
            )
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
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                color = MaterialTheme.colorScheme.error
            ) },
            line2 = { Text(
                text = media.creator,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                color = MaterialTheme.colorScheme.error
            ) }
        )

        //Relink Button
        IconButton(onClick = onRelinkClick) {
            Icon(
                imageVector = Icons.Default.AddLink,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Link New URI"
            )
        }

        //Delete Button
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = "Link New URI"
            )
        }
    }
}