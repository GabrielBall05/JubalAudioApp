package com.devball.jubalaudio.util

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun ObserveUiEvents(
    eventFlow: Flow<UiEvent>,
    onEvent: (UiEvent) -> Unit = {} //For custom gravity/positioning logic
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(eventFlow, lifecycleOwner) {
        var currentToast: Toast? = null

        eventFlow
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collect { event ->
                onEvent(event)

                when (event) {
                    is UiEvent.ShowToast -> {
                        currentToast?.cancel()
                        currentToast = Toast.makeText(context, event.message, event.length)

                        //Maybe later: Change global position here

                        currentToast?.show()
                    }
                    //Other global ui events
                }
            }
    }
}