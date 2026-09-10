package com.devball.jubalaudio.ui

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Playlists : Screen("playlists", "Playlists")
    object PlaylistDetails : Screen("playlist_details/{id}", "Playlist Details") {
        fun createRoute(id: Int) = "playlist_details/$id"
    }
    object Settings : Screen("settings", "Settings")

    object Player : Screen("player", "Player")
}