package com.devball.jubalaudio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = OriginalPlaylistEntity.TABLE_NAME)
data class OriginalPlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,        //Auto-incrementing primary key
    val mediaId: Int,       //Actual mediaId of the media item
    val sequenceOrder: Int, //To maintain exact order of original playlist
) {
    companion object {
        const val TABLE_NAME = "original_playlist_persistence"
    }
}