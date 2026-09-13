package com.devball.jubalaudio.ui.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableTopBar(
    modifier: Modifier = Modifier,
    title: String,
    titlePadding: PaddingValues = PaddingValues(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholderText: String,
    actions: @Composable RowScope.() -> Unit
) {
    var isSearching by rememberSaveable { mutableStateOf(false) }

    //Expand if there's already a query
    LaunchedEffect(Unit) {
        if (searchQuery.isNotEmpty()) isSearching = true
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = isSearching,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.weight(1f),
            label = "SearchTransition"
        ) { searching ->
            if (searching) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isSearching = false
                        onSearchQueryChange("") //Clear search on collapse
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Collapse Search"
                        )
                    }
                    SearchBar(
                        value = searchQuery,
                        placeHolderText = placeholderText,
                        modifier = Modifier.weight(1f),
                        onClear = {
                            if (searchQuery.isEmpty()) isSearching = false
                            else onSearchQueryChange("")
                        },
                        onValueChange = onSearchQueryChange
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .padding(titlePadding)
                            .weight(1f),
                        text = title,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Open Search"
                        )
                    }
                }
            }
        }

        //Additional buttons to put on top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}