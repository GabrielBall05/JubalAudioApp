package com.example.offlineplayer.ui.components.common

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.InfoColumnMarquee(
    modifier: Modifier = Modifier,
    mainText: String,
    mainTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    subText: String? = null,
    subTextStyle: TextStyle = MaterialTheme.typography.bodySmall,
    textColor: Color = Color.Unspecified
) {
    Column(
        modifier = modifier
            .weight(1f)
            .padding(horizontal = 12.dp)
    ) {
        //Line 1
        Text(
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            text = mainText,
            style = mainTextStyle,
            maxLines = 1,
            color = textColor
        )
        //Line 2
        subText?.let { subText ->
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                text = subText,
                style = subTextStyle,
                maxLines = 1,
                color = textColor
            )
        }
    }
}