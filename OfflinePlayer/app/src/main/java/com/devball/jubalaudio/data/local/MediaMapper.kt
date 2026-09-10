package com.devball.jubalaudio.data.local

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.util.UUID

fun MediaEntity.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(this.mediaId.toString())
        .setUri(this.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.title)
                .setArtist(this.creator)
                .setArtworkUri(this.artworkUri?.toUri())
                .build()
        )
        .build()
}

fun MediaItem.asManualQueueItem(): MediaItem {
    // Preserve any existing extras, but inject our manual queue identifiers
    val newExtras = Bundle(this.mediaMetadata.extras ?: Bundle()).apply {
        putString("ORIGINAL_MEDIA_ID", this@asManualQueueItem.mediaId)
        putBoolean("IS_MANUAL_QUEUE", true)
    }

    return this.buildUpon()
        .setMediaId("queue_${UUID.randomUUID()}_${this.mediaId}")
        .setMediaMetadata(
            this.mediaMetadata.buildUpon()
                .setExtras(newExtras)
                .build()
        )
        .build()
}