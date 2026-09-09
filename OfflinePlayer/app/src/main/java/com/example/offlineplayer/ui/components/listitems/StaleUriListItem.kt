package com.example.offlineplayer.ui.components.listitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.data.local.MediaEntity
import com.example.offlineplayer.ui.components.common.IconPulseEffect
import com.example.offlineplayer.ui.components.common.IconWithTooltip
import com.example.offlineplayer.ui.components.common.InfoColumnMarquee
import com.example.offlineplayer.ui.components.common.SelectionIcon
import com.example.offlineplayer.ui.components.common.SurfacedImage

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
        InfoColumnMarquee(
            mainText = media.title,
            subText = media.creator,
            textColor = MaterialTheme.colorScheme.error
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