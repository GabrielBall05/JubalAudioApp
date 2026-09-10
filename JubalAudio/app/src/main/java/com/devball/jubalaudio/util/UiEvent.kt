package com.devball.jubalaudio.util

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}