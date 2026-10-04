package com.devball.jubalaudio.viewmodels.events

import android.widget.Toast

sealed interface UiEvent {
    data class ShowToast(
        val message: String,
        val length: Int = Toast.LENGTH_SHORT
    ) : UiEvent
}