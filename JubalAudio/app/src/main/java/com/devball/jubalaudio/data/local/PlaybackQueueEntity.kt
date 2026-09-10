package com.devball.jubalaudio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(PlaybackQueueEntity.TABLE_NAME)
data class PlaybackQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,        //Auto-incrementing primary key
    val mediaId: Int,       //Actual mediaId of the Media Item referencing Room
    val isManual: Boolean,  //To rebuild the manual queue state
    val sequenceOrder: Int  //To maintain exact order of timeline
) {
    companion object {
        const val TABLE_NAME = "playback_persistence_queue"
    }
}