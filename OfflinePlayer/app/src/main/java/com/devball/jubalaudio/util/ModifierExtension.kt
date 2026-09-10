package com.devball.jubalaudio.util

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.indicatorBorder(
    enabled: Boolean,
    color: Color,
    borderWidth: Dp = 2.dp
): Modifier = if (enabled) {
    this.border(
        width = borderWidth,
        color = color,
        shape = CircleShape
    )
} else {
    this
}