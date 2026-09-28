package com.devball.jubalaudio.player

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.devball.jubalaudio.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerWidget : GlanceAppWidget() {
    //For Glance to manager configuration states via internal DataStore instance
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val imageUriString = prefs[ImageUriKey] ?: ""
            val title = prefs[TitleKey] ?: "Not Playing"
            val artist = prefs[ArtistKey] ?: ""
            val isPlaying = prefs[IsPlayingKey] ?: false

            //TODO: IMAGE

            //Asynchronously load and downscale image URI into a Bitmap
            val imageBitmap by produceState<Bitmap?>(initialValue = null, key1 = imageUriString) {
                if (imageUriString.isNotEmpty()) {
                    val imageLoader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(imageUriString)
                        .size(175, 175)
                        .build()
                    val result = imageLoader.execute(request)
                    value = if (result is SuccessResult) result.image.toBitmap() else null
                } else {
                    value = null
                }
            }

            //Actual Widget Layout
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                //Image
                imageBitmap?.let { bitmap ->
                    Image(
                        provider = ImageProvider(bitmap),
                        modifier = GlanceModifier
                            .size(48.dp)
                            .cornerRadius(4.dp)
                            .background(GlanceTheme.colors.surfaceVariant),
                        contentDescription = "Artwork Image"
                    )
                }

                //Title & Artist
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(start = if (imageBitmap != null) 8.dp else 0.dp)
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = artist,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )
                }

                //Playback Controls
                Row(
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    val buttonModifier = GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)

                    //Previous
                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_previous),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                        contentDescription = "Previous",
                        modifier = buttonModifier.clickable(actionRunCallback<PreviousActionCallback>())
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))

                    //Play/Pause Toggle
                    Image(
                        provider = ImageProvider(
                            if (isPlaying) R.drawable.ic_pause
                            else R.drawable.ic_play_arrow
                        ),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = buttonModifier.clickable(actionRunCallback<PlayPauseActionCallback>())
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))

                    //Next
                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_next),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                        contentDescription = "Next",
                        modifier = buttonModifier.clickable(actionRunCallback<NextActionCallback>())
                    )
                }
            }
        }
    }

    companion object {
        val ImageUriKey = stringPreferencesKey("widget_image_uri")
        val TitleKey = stringPreferencesKey("widget_media_title")
        val ArtistKey = stringPreferencesKey("widget_media_artist")
        val IsPlayingKey = booleanPreferencesKey("widget_media_is_playing")
    }
}


//TODO: Make a callback for clicking the entire widget to open the app into the expanded player screen


class PlayPauseActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.Main) {
            getMediaControllerManager(context).togglePlayPause()
        }
    }
}

class PreviousActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.Main) {
            getMediaControllerManager(context).seekToPrevious(true)
        }
    }
}

class NextActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.Main) {
            getMediaControllerManager(context).seekToNext()
        }
    }
}

private fun getMediaControllerManager(context: Context): MediaControllerManager {
    return EntryPointAccessors.fromApplication(
        context = context.applicationContext,
        entryPoint = WidgetEntryPoint::class.java
    ).mediaControllerManager()
}