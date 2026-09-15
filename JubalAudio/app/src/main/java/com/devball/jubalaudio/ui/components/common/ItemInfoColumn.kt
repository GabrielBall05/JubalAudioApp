package com.devball.jubalaudio.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.ItemInfoColumn(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(horizontal = 12.dp),
    line1: @Composable () -> Unit,
    line2: @Composable (() -> Unit)? = null,
    line3: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .weight(1f)
            .padding(paddingValues),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        line1()
        line2?.let { line2() }
        line3?.let { line3() }
    }
}