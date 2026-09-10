package com.devball.jubalaudio.ui.components.listitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devball.jubalaudio.data.local.PlaylistEntity
import com.devball.jubalaudio.ui.components.common.InfoColumnMarquee
import com.devball.jubalaudio.ui.components.common.SelectionIcon
import com.devball.jubalaudio.ui.components.common.SurfacedImage

@Composable
fun PlaylistPickerListItem(
    playlist: PlaylistEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Artwork
        SurfacedImage(
            model = playlist.coverUri,
            contentDescription = "Cover Image",
            fallbackIcon = Icons.Default.LibraryMusic,
        )

        //Playlist Name
        InfoColumnMarquee(
            mainText = playlist.name,
            mainTextStyle = MaterialTheme.typography.titleLarge
        )

        //Checkbox
        SelectionIcon(isSelected = isSelected)
    }
}