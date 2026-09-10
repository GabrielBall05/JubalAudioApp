package com.devball.jubalaudio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    //CREATE playlist
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    //READ - Get playlist by id
    @Query("SELECT * FROM ${PlaylistEntity.TABLE_NAME} WHERE playlistId = :id")
    fun getPlaylistById(id: Int): Flow<PlaylistEntity?>

    //READ - Get all playlists
    @Query("SELECT * FROM ${PlaylistEntity.TABLE_NAME} ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    //UPDATE playlist
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    //DELETE playlist
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    //======================== PLAYLIST MEDIA ITEMS ========================//

    //CREATE - Add media to playlists
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMediaToPlaylists(items: List<PlaylistMediaItem>)

    //READ - Get all media in playlist
    @Query("""
        SELECT MI.* FROM ${MediaEntity.TABLE_NAME} AS MI
        INNER JOIN ${PlaylistMediaItem.TABLE_NAME} AS PMI
            ON MI.mediaId = PMI.mediaId
        WHERE PMI.playlistId = :playlistId
        ORDER BY PMI.positionInPlaylist ASC
    """)
    fun getMediaInPlaylist(playlistId: Int): Flow<List<MediaEntity>>

    //Get all media not in given playlist
    @Query("""
        SELECT * FROM ${MediaEntity.TABLE_NAME} AS M
        WHERE NOT EXISTS (
            SELECT 1 FROM ${PlaylistMediaItem.TABLE_NAME} AS P
            WHERE P.mediaId = M.mediaId 
            AND P.playlistId = :playlistId
        )
        ORDER BY title ASC
    """)
    suspend fun getMediaNotInPlaylist(playlistId: Int): List<MediaEntity>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM ${PlaylistMediaItem.TABLE_NAME}
            WHERE playlistId = :playlistId AND mediaId = :mediaId
        )
    """)
    fun isMediaInPlaylist(mediaId: Int, playlistId: Int): Flow<Boolean>

    @Query("""
        SELECT P.* 
        FROM ${PlaylistEntity.TABLE_NAME} P
        LEFT JOIN ${PlaylistMediaItem.TABLE_NAME} PMI
            ON P.playlistId = PMI.playlistId
            AND PMI.mediaId IN (:mediaIds)
        GROUP BY P.playlistId
        HAVING COUNT(DISTINCT PMI.mediaId) < (:size)
    """)
    suspend fun getPlaylistsNotHavingMediaList(mediaIds: List<Int>, size: Int = mediaIds.size): List<PlaylistEntity>

    //Get playlist item count
    @Query("SELECT COUNT(*) FROM ${PlaylistMediaItem.TABLE_NAME} WHERE playlistId = :playlistId")
    fun getPlaylistItemCount(playlistId: Int): Flow<Int>

    @Query("""
    UPDATE ${PlaylistMediaItem.TABLE_NAME} 
    SET positionInPlaylist = CASE mediaId
        WHEN :fromMediaId THEN :toPos
        WHEN :toMediaId THEN :fromPos
    END
    WHERE playlistId = :playlistId AND mediaId IN (:fromMediaId, :toMediaId)
""")
    suspend fun moveMediaItemPositionInPlaylist(playlistId: Int, fromMediaId: Int, toMediaId: Int, fromPos: Int, toPos: Int)

    //DELETE - Remove media from playlist
    @Query("""
        DELETE FROM ${PlaylistMediaItem.TABLE_NAME}
        WHERE mediaId IN (:mediaIds)
        AND playlistId = :playlistId
    """)
    suspend fun removeMediaFromPlaylist(mediaIds: List<Int>, playlistId: Int)

    //Get the max position in a given playlist for ordering new media items
    @Query("""
        SELECT MAX(positionInPlaylist)
        FROM ${PlaylistMediaItem.TABLE_NAME}
        WHERE playlistId = :playlistId
    """)
    suspend fun getMaxPositionInPlaylist(playlistId: Int): Int?
}