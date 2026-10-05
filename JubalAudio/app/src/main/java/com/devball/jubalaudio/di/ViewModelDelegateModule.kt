package com.devball.jubalaudio.di

import com.devball.jubalaudio.delegates.MediaActionsDelegate
import com.devball.jubalaudio.delegates.MediaActionsDelegateImpl
import com.devball.jubalaudio.delegates.PlaylistActionsDelegate
import com.devball.jubalaudio.delegates.PlaylistActionsDelegateImpl
import com.devball.jubalaudio.delegates.SearchDelegate
import com.devball.jubalaudio.delegates.SearchDelegateImpl
import com.devball.jubalaudio.delegates.SelectionDelegate
import com.devball.jubalaudio.delegates.SelectionDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelDelegateModule {
    @Binds
    @ViewModelScoped
    abstract fun bindSearchDelegate(impl: SearchDelegateImpl): SearchDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindSelectionDelegate(impl: SelectionDelegateImpl): SelectionDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindMediaActionsDelegate(impl: MediaActionsDelegateImpl): MediaActionsDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindPlaylistActionsDelegate(impl: PlaylistActionsDelegateImpl): PlaylistActionsDelegate
}