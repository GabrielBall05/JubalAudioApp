package com.example.offlineplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.example.offlineplayer.data.local.PlaylistEntity
import com.example.offlineplayer.ui.components.dialogs.ConfirmationDialog
import com.example.offlineplayer.ui.components.listitems.QueueItem
import com.example.offlineplayer.util.SwipeDismissable

@Composable
fun QueueScreen(
    infinitePlaybackSetting: Boolean,
    activePlaylist: PlaylistEntity?,
    currentlyPlaying: MediaItem?,
    manualQueue: List<MediaItem>,
    upNext: List<MediaItem>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onClearQueue: () -> Unit,
    onMoveManualQueueItem: (Int, Int) -> Unit,
    onMoveUpNextItem: (Int, Int) -> Unit,
    onManualQueueSkipToIndex: (Int) -> Unit,
    onUpNextSkipToIndex: (Int) -> Unit,
    onRemoveItemAtIndex: (Int, Boolean) -> Unit
) {
    BackHandler(onBack = onDismiss)

    var showClearQueueConfirmation by rememberSaveable { mutableStateOf(false) }

    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.scrollToItem(0)
        }
    }

    //Screen UI
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState
    ) {
        //Currently Playing
        currentlyPlaying?.let { current ->
            item {
                //Section Header
                Text(
                    text = "Now Playing",
                    modifier = Modifier.padding(all = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                //Row Item Content
                QueueItem(
                    item = current,
                    isFirst = true,
                    isLast = true,
                    isPlaying = true,
                    onMoveUp = {  },
                    onMoveDown = {  }
                )
            }
        }

        //Queue
        if (manualQueue.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Section Header
                    Text(
                        text = "Queue",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    //Clear Queue Option
                    OutlinedButton(onClick = { showClearQueueConfirmation = true }) {
                        Text("Clear Queue")
                    }
                }
            }
            itemsIndexed(
                items = manualQueue,
                key = { _, item -> "manual_${item.mediaId}_${item.hashCode()}" }
            ) { index, item ->
                SwipeDismissable(onDismiss = { onRemoveItemAtIndex(index, true) }) {
                    QueueItem(
                        item = item,
                        isFirst = (index == 0),
                        isLast = (index == manualQueue.size - 1),
                        onClick = {
                            onManualQueueSkipToIndex(index)
                            scrollToTopTrigger++
                        },
                        onMoveUp = { onMoveManualQueueItem(index, index - 1) },
                        onMoveDown = { onMoveManualQueueItem(index, index + 1) }
                    )
                }
            }
        }

        //Up Next
        if (upNext.isNotEmpty()) {
            item {
                //Section Header
                Text(
                    text = "Up Next",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            itemsIndexed(
                items = upNext,
                key = { _, item -> "upnext_${item.mediaId}_${item.hashCode()}" }
            ) { index, item ->
                SwipeDismissable(onDismiss = { onRemoveItemAtIndex(index, false) }) {
                    QueueItem(
                        item = item,
                        isFirst = (index == 0),
                        isLast = (index == upNext.size - 1),
                        onClick = {
                            onUpNextSkipToIndex(index)
                            scrollToTopTrigger++
                        },
                        onMoveUp = { onMoveUpNextItem(index, index - 1) },
                        onMoveDown = { onMoveUpNextItem(index, index + 1) }
                    )
                }
            }
        }

        //Infinite Playback message
        item {
            Text(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                text = buildAnnotatedString {
                    if (infinitePlaybackSetting && activePlaylist != null) {
                        append("${activePlaylist.name} will repeat when this message is reached. Full playlist will be fetched.")
                    }
                    else if (!infinitePlaybackSetting && activePlaylist != null) {
                        append("Infinite Playback is turned off. ${activePlaylist.name} will not be repeated.")
                    }
                    else {
                        append("End of timeline.")
                        if (infinitePlaybackSetting) {
                            append(" No currently active playlist to repeat.")
                        }
                    }
                },
                textAlign = TextAlign.Center
            )
        }
    }


    //Show confirmation dialog if user hits Clear Queue button
    if (showClearQueueConfirmation) {
        ConfirmationDialog(
            title = "Clear Queue?",
            text = "Are you sure you want to clear the queue?",
            confirmText = "Clear",
            onDismiss = { showClearQueueConfirmation = false },
            onConfirm = {
                onClearQueue()
                showClearQueueConfirmation = false
            }
        )
    }
}