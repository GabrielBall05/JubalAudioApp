package com.devball.jubalaudio.data.local

import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

private const val REORDER_PLAYLIST_AFTER_DELETE = """
    CREATE TRIGGER IF NOT EXISTS reorder_playlist_after_delete
    AFTER DELETE ON ${PlaylistMediaItem.TABLE_NAME}
    BEGIN
        UPDATE ${PlaylistMediaItem.TABLE_NAME}
        SET positionInPlaylist = positionInPlaylist - 1
        WHERE playlistId = OLD.playlistId
        AND positionInPlaylist > OLD.positionInPlaylist;
    END;
"""

val DatabaseTriggerCallback = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        setupTriggers(db, "onCreate")
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        setupTriggers(db, "onDestructiveMigration")
    }

    private fun setupTriggers(db: SupportSQLiteDatabase, lifecycleStage: String) {
        try {
            db.execSQL(REORDER_PLAYLIST_AFTER_DELETE)
            Log.d("OfflineAudioSuite", "Successfully created reorder trigger during $lifecycleStage.")
        } catch (e: Exception) {
            Log.e("OfflineAudioSuite", "Failed to create trigger during $lifecycleStage: ${e.message}")
        }
    }
}