package com.devball.jubalaudio.di

import android.content.Context
import androidx.room.Room
import com.devball.jubalaudio.data.local.AppDatabase
import com.devball.jubalaudio.data.local.DatabaseTriggerCallback
import com.devball.jubalaudio.data.local.MediaDao
import com.devball.jubalaudio.data.local.PlaybackQueueDao
import com.devball.jubalaudio.data.local.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class) //So database lives as long as app does
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(true)
            .addCallback(DatabaseTriggerCallback)
            .build()
    }

    @Provides
    fun provideMediaDao(database: AppDatabase): MediaDao {
        return database.mediaDao()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun providePlaybackQueueDao(database: AppDatabase): PlaybackQueueDao {
        return database.playbackQueueDao()
    }
}