package com.devball.jubalaudio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devball.jubalaudio.data.local.dao.MediaDao
import com.devball.jubalaudio.data.local.dao.PlaybackQueueDao
import com.devball.jubalaudio.data.local.dao.PlaylistDao
import com.devball.jubalaudio.data.local.entity.MediaEntity
import com.devball.jubalaudio.data.local.entity.OriginalPlaylistEntity
import com.devball.jubalaudio.data.local.entity.PlaybackQueueEntity
import com.devball.jubalaudio.data.local.entity.PlaylistEntity
import com.devball.jubalaudio.data.local.entity.PlaylistMediaItem

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