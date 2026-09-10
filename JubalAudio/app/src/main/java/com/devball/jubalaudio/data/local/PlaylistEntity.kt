package com.devball.jubalaudio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PlaylistEntity.TABLE_NAME)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Int = 0,    //Auto-incremented and auto-generated primary key
    val name: String,           //Name of playlist
    val description: String?,   //Optional description
    val dateCreated: Long,      //Date added
    val coverUri: String?     //Local path to associated image
) {
    companion object {
        const val TABLE_NAME = "playlists"
    }
}