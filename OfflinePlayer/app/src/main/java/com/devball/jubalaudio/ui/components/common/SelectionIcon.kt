package com.devball.jubalaudio.ui.components.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SelectionIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
        contentDescription = if (isSelected) "Select Item" else "Deselect Item",
        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
        modifier = modifier
    )
}