package com.devball.jubalaudio.widget

import com.devball.jubalaudio.player.MediaControllerManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun mediaControllerManager(): MediaControllerManager
}