package com.devball.jubalaudio.viewmodels

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devball.jubalaudio.viewmodels.events.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


interface UiActionHandler {
    fun launchWithLoading(block: suspend () -> Unit)
    fun launchWithoutLoading(block: suspend () -> Unit)
    fun showToast(message: String, length: Int = Toast.LENGTH_SHORT)
    fun showSnackbar(message: String)
}

abstract class BaseViewModel : ViewModel(), UiActionHandler {
    protected val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    //Channel for handling UI Events (Toasts)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()


    //Send Toast UI Event over the Channel
    override fun showToast(message: String, length: Int) {
        viewModelScope.launch {
            _uiEvent.send((UiEvent.ShowToast(message, length)))
        }
    }

    //Send Snackbar UI Event over the Channel
    override fun showSnackbar(message: String) {
        viewModelScope.launch {
            //TODO: When the time comes to add snack bars, make .ShowSnackbar UiEvent and implement this
        }
    }

    //Launch Coroutine and show loading screen
    override fun launchWithLoading(block: suspend () -> Unit) {
        _isLoading.value = true //Show loading screen
        viewModelScope.launch {
            try { block() } //Perform given operation
            catch (e: Exception) {
                e.printStackTrace()
                //Notify user of a failure
                showToast(e.localizedMessage ?: "An unexpected error occurred.")
            }
            finally { _isLoading.value = false } //Hide loading screen
        }
    }

    //Launch Coroutine without showing loading screen
    override fun launchWithoutLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } //Perform given operation
            catch (e: Exception) {
                e.printStackTrace()
                //Notify user of a failure
                showToast(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }
}
