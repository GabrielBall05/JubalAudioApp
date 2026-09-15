package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.MediaEntity
import com.devball.jubalaudio.ui.components.common.ItemInfoColumn
import com.devball.jubalaudio.ui.components.common.SelectionIcon
import com.devball.jubalaudio.ui.components.common.SurfacedImage

@Composable
fun MediaPickerListItem(
    media: MediaEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
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

        //Checkbox
        SelectionIcon(isSelected = isSelected)
    }
}