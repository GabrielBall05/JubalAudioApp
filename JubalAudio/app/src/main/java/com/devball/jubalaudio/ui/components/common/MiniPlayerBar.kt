package com.devball.jubalaudio.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devball.jubalaudio.ui.viewmodels.MainViewModel

@Composable
fun MiniPlayerBar(
    viewModel: MainViewModel, //TODO: Pass individual items from MainActivity instead of the whole viewmodel
    onExpand: () -> Unit
) {
    val currentMedia by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    //If nothing is loaded in the player (no media items), don't show the bar at all
    if (currentMedia == null) return

    //Calculate progress
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Artwork
            SurfacedImage(
                model = currentMedia?.mediaMetadata?.artworkUri?.toString(),
                contentDescription = "Artwork Image",
                sizeInDp = 50.dp
            )

            //Title & Creator
            ItemInfoColumn(
                paddingValues = PaddingValues(start = 8.dp),
                line1 = { Text(
                    text = currentMedia?.mediaMetadata?.title?.toString() ?: "Unknown Title",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis
                ) },
                line2 = { Text(
                    text = currentMedia?.mediaMetadata?.artist?.toString() ?: "Unknown Creator",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis
                ) }
            )

            //Controls
            IconButton(onClick = { viewModel.seekToPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track")
            }
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
            }
            IconButton(onClick = { viewModel.seekToNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Track")
            }
        }

        //Progress Line
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(3.dp), //Very thin line
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
            drawStopIndicator = {}
        )
    }
}