package com.example.offlineplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    //Create list of media items
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaList(mediaList: List<MediaEntity>): List<Long>

    //----------READ----------
    //Get all media items
    @Query("SELECT * FROM ${MediaEntity.TABLE_NAME} ORDER BY title ASC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM ${MediaEntity.TABLE_NAME} WHERE mediaId = :id")
    fun getMediaById(id: Int): MediaEntity?

    //----------UPDATE----------
    //Update single media item
    @Update
    suspend fun updateMedia(media: MediaEntity)

    //Update creator bulk
    @Query("""
        UPDATE ${MediaEntity.TABLE_NAME}
        SET creator = :creator
        WHERE mediaId IN (:ids)
    """)
    suspend fun updateCreatorBulk(creator: String, ids: List<Int>)

    //Update artwork bulk
    @Query("""
        UPDATE ${MediaEntity.TABLE_NAME}
        SET artworkUri = :artworkUri
        WHERE mediaId IN (:ids)
    """)
    suspend fun updateArtworkBulk(artworkUri: String?, ids: List<Int>)

    @Query("""
        UPDATE ${MediaEntity.TABLE_NAME}
        SET uri = :newUri, isStaleUri = 0 
        WHERE mediaId = :id
    """)
    suspend fun updateMediaUri(id: Int, newUri: String)

    //Delete list of media items (by mediaId)
    @Query("DELETE FROM ${MediaEntity.TABLE_NAME} WHERE mediaId IN (:mediaIds)")
    suspend fun deleteMediaList(mediaIds: List<Int>)


    @Query("SELECT * FROM ${MediaEntity.TABLE_NAME}")
    suspend fun getAllMediaOnce(): List<MediaEntity>

    @Query("""
        UPDATE ${MediaEntity.TABLE_NAME}
        SET isStaleUri = 1
        WHERE mediaId in (:ids)
    """)
    suspend fun markAsStale(ids: List<Int>)
}