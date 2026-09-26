package com.devball.jubalaudio.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
fun LoadingDialog(
    modifier: Modifier = Modifier,
    removeDim: Boolean = false,
    loadingText: String = "Loading..."
) {
    Dialog(
        onDismissRequest = { /* Force user to wait */ },
        properties = DialogProperties(
            dismissOnBackPress = false, //Prevent closing with back button
            dismissOnClickOutside = false, //Prevent closing by tapping outside
            usePlatformDefaultWidth = false //Allows dialog to span full screen if needed
        )
    ) {
        //Allows for removing or adjusting background dim
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            if (removeDim) window.setDimAmount(0f)
        }

        Column(
            modifier = modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = loadingText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}