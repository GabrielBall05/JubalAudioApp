package com.devball.jubalaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devball.jubalaudio.ui.Screen
import com.devball.jubalaudio.ui.components.common.MiniPlayerBar
import com.devball.jubalaudio.ui.screens.ExpandedPlayerScreen
import com.devball.jubalaudio.ui.screens.HomeScreen
import com.devball.jubalaudio.ui.screens.PlaylistDetailsScreen
import com.devball.jubalaudio.ui.screens.PlaylistsScreen
import com.devball.jubalaudio.ui.screens.SettingsScreen
import com.devball.jubalaudio.ui.theme.JubalAudioTheme
import com.devball.jubalaudio.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devball.jubalaudio.util.ObserveUiEvents

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        @Suppress("DEPRECATION")
        ComposeFoundationFlags.isNonComposedClickableEnabled = false

        //FOR GOOGLE PLAY SCREENSHOTS ONLY - TODO: REMOVE FROM PRODUCTION
        //val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        //windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        //windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            JubalAudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainViewModel: MainViewModel = hiltViewModel()) {
    //Ui Event Observer
    ObserveUiEvents(eventFlow = mainViewModel.uiEvent)

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hideBottomBars = currentRoute == Screen.Player.route //Hide bottom bars if Player is expanded

    //State for ExpandedPlayerScreen Sheet
    var showExpandedPlayerSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isPlaying by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val activePlaylistId by mainViewModel.currentPlaylistId.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (!hideBottomBars) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MiniPlayerBar(
                        viewModel = mainViewModel,
                        onExpand = { showExpandedPlayerSheet = true }
                    )
                    BottomNavigationBar(navController)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(if (hideBottomBars) PaddingValues(0.dp) else innerPadding)
        ) {
            composable(route = Screen.Home.route) {
                HomeScreen(
                    onPlayMediaClick = { mainViewModel.playMediaNow(it) },
                    onAddToQueueClick = { mainViewModel.addMediaToQueue(it) }
                )
            }

            composable(route = Screen.Playlists.route) {
                PlaylistsScreen(
                    navController = navController,
                    onPlayPlaylistClick = { mainViewModel.playPlaylist(it) },
                    onAddPlaylistToQueueClick = { mainViewModel.addPlaylistToQueue(it) }
                )
            }

            composable(route = Screen.Settings.route) {
                SettingsScreen(
                    onClearPlayer = { mainViewModel.clearPlayer() }
                )
            }

            composable(
                route = Screen.PlaylistDetails.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) {
                PlaylistDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onPlayMediaClick = { mainViewModel.playMediaNow(it) },
                    onAddToQueueClick = { mainViewModel.addMediaToQueue(it) },
                    onPlayPlaylistClick = { playlistId, startItemId ->
                        mainViewModel.playPlaylist(playlistId, startItemId)
                    },
                    onTogglePlayPauseClick = { mainViewModel.togglePlayPause() },
                    isActivePlaylistPlaying = isPlaying,
                    activePlaylistId = activePlaylistId
                )
            }
        }

        if (showExpandedPlayerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExpandedPlayerSheet = false },
                sheetState = sheetState,
                dragHandle = null,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = { WindowInsets(0) }
            ) {
                ExpandedPlayerScreen(
                    viewModel = mainViewModel,
                    onCollapse = { showExpandedPlayerSheet = false }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Playlists, Screen.Settings)

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    val icon = when(screen) {
                        Screen.Home -> Icons.Default.Home
                        Screen.Playlists -> Icons.Default.LibraryMusic
                        Screen.Settings -> Icons.Default.Settings
                        else -> Icons.Default.Favorite
                    }
                    Icon(icon, contentDescription = screen.title)
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
