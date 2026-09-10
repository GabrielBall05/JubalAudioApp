package com.devball.jubalaudio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = PlaylistMediaItem.TABLE_NAME,
    primaryKeys = ["playlistId", "mediaId"], //Composite primary key
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistMediaItem(
    val playlistId: Int,        //Id for playlist
    val mediaId: Int,           //Id for media
    val positionInPlaylist: Int //For custom ordering within playlist
) {
    companion object {
        const val TABLE_NAME = "playlist_media_items"
    }
}