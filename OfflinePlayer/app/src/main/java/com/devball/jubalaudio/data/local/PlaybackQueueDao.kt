package com.devball.jubalaudio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PlaybackQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(items: List<PlaybackQueueEntity>)

    @Query("DELETE FROM ${PlaybackQueueEntity.TABLE_NAME}")
    suspend fun clearQueue()

    @Transaction
    suspend fun replaceQueue(items: List<PlaybackQueueEntity>) {
        clearQueue()
        insertQueue(items)
    }

    @Query("SELECT * FROM ${PlaybackQueueEntity.TABLE_NAME} ORDER BY sequenceOrder")
    suspend fun getSavedQueue(): List<PlaybackQueueEntity>



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOriginalPlaylist(items: List<OriginalPlaylistEntity>)

    @Query("DELETE FROM ${OriginalPlaylistEntity.TABLE_NAME}")
    suspend fun clearOriginalPlaylist()

    @Transaction
    suspend fun replaceOriginalPlaylist(items: List<OriginalPlaylistEntity>) {
        clearOriginalPlaylist()
        insertOriginalPlaylist(items)
    }

    @Query("SELECT * FROM ${OriginalPlaylistEntity.TABLE_NAME} ORDER BY sequenceOrder ASC")
    suspend fun getOriginalPlaylist(): List<OriginalPlaylistEntity>
}