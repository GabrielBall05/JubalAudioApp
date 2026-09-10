package com.devball.jubalaudio.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SwipeDismissableBackground(
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    iconSize: Dp = 50.dp
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        imageVector?.let {
            Icon(
                modifier = Modifier
                    .size(iconSize)
                    .padding(8.dp),
                imageVector = imageVector,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = "Remove",
            )
        }
    }
}