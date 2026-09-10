package com.devball.jubalaudio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.util.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    //Channel for handling UI Events (Toasts)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    //Send UI Event over the Channel
    protected fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    //Launch Coroutine and show loading screen
    protected fun launchWithLoading(block: suspend () -> Unit) {
        _isLoading.value = true //Show loading screen
        viewModelScope.launch {
            try { block() } //Perform given operation
            catch (e: Exception) {
                e.printStackTrace()
                _uiEvent.send( //Notify user of a failure
                    UiEvent.ShowToast(e.localizedMessage ?: "An unexpected error occurred.")
                )
            }
            finally { _isLoading.value = false } //Hide loading screen
        }
    }

    //Launch Coroutine without showing loading screen
    protected fun launchWithoutLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } //Perform given operation
            catch (e: Exception) {
                e.printStackTrace()
                _uiEvent.send( //Notify user of a failure
                    UiEvent.ShowToast(e.localizedMessage ?: "An unexpected error occurred.")
                )
            }
        }
    }
}
