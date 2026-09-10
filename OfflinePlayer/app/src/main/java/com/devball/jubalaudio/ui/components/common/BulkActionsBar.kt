package com.devball.jubalaudio.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BulkActionsBar(
    modifier: Modifier = Modifier,
    isAnySelected: Boolean = false,
    isAllSelected: Boolean = false,
    onToggleAllClick: () -> Unit,
    onClearSelectionClick: () -> Unit,
    actionButtons: @Composable (RowScope.() -> Unit)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Absolute.Left
    ) {
        //Toggle All Button
        IconButton(onClick = onToggleAllClick) {
            SelectionIcon(isAllSelected)
        }

        //Bulk Actions
        AnimatedVisibility(visible = isAnySelected) { //Only show if 1 or more items selected
            Row {
                Row(modifier = Modifier.weight(1f)) {
                    actionButtons()
                }
                //Clear selection button
                Row {
                    TextButton(onClick = onClearSelectionClick) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}