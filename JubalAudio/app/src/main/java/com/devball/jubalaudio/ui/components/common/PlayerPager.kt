package com.devball.jubalaudio.ui.components.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerPager(
    previousMediaItem: MediaItem?,
    currentMediaItem: MediaItem?,
    nextMediaItem: MediaItem?,
    onSwipeToPrevious: () -> Unit,
    onSwipeToNext: () -> Unit,
    modifier: Modifier = Modifier,
    additionalButtons: @Composable (() -> Unit)? = null
) {
    val hasPrevious = previousMediaItem != null
    val hasNext = nextMediaItem != null

    //Compute dynamic page count
    val pageCount = when {
        hasPrevious && hasNext -> 3
        hasPrevious || hasNext -> 2
        else -> 1
    }

    //Map current media item's page position based on whether previous exists
    val currentPageIndex = if (hasPrevious) 1 else 0

    val pagerState = rememberPagerState(
        initialPage = currentPageIndex, //Use computed dynamic current item index
        pageCount = { pageCount } //Use computed dynamic page count
    )

    //Keep pager centered on current page when track changes or state updates
    LaunchedEffect(currentPageIndex, currentMediaItem) {
        pagerState.scrollToPage(currentPageIndex)
    }

    //Listen for completed swipes to adjacent pages
    LaunchedEffect(pagerState.settledPage) {
        //Invoke appropriate callback then ensure current page is always center
        if (pagerState.settledPage < currentPageIndex) {
            onSwipeToPrevious()
            pagerState.scrollToPage(currentPageIndex)
        } else if (pagerState.settledPage > currentPageIndex) {
            onSwipeToNext()
            pagerState.scrollToPage(currentPageIndex)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        //Map page index to correct media item
        val item = when {
            hasPrevious && page == 0 -> previousMediaItem
            hasPrevious && page == 1 -> currentMediaItem
            !hasPrevious && page == 0 -> currentMediaItem
            else -> nextMediaItem
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            SurfacedImage(
                model = item?.mediaMetadata?.artworkUri?.toString(),
                contentDescription = "Artwork Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeightedColumn(paddingValues = PaddingValues(end = 8.dp)) {
                    Text(
                        text = item?.mediaMetadata?.title?.toString() ?: "Unknown Title",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = item?.mediaMetadata?.artist?.toString() ?: "Unknown Creator",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }

                additionalButtons?.invoke() //Display any additional buttons given
            }
        }
    }
}