package com.devball.jubalaudio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = MediaEntity.TABLE_NAME)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val mediaId: Int = 0,           //Auto-incremented and auto-generated primary key.
                                    //Default 0 w/ autoGen - assign next available int to mediaID in DB table
    val uri: String,                //Location of the file
    val title: String,              //Title of the song, podcast, audiobook, etc
    val creator: String,            //Name of artist, podcaster, author, etc
    val duration: Long,             //Length in ms
    val fileName: String,           //Actual name of the file
    val dateAdded: Long,            //Date added (used for recently added section)
    val mimeType: String?,          //File type (ex: mp3, wav)
    val artworkUri: String?,        //Local path to associated image
    val isStaleUri: Boolean = false //Tracks URI integrity
) {
    companion object {
        const val TABLE_NAME = "media_items"
    }
}