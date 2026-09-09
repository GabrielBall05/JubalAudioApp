package com.example.offlineplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaEntity::class,
        PlaylistEntity::class,
        PlaylistMediaItem::class,
        PlaybackQueueEntity::class,
        OriginalPlaylistEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    //Connect Database to Dao
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackQueueDao(): PlaybackQueueDao

    companion object {
        const val DATABASE_NAME = "offline_player_db"
    }
}