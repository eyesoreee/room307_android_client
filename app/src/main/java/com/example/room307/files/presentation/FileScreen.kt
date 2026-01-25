package com.example.room307.files.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.room307.ui.SearchBar

@Composable
fun FileScreen(
    modifier: Modifier = Modifier,
    viewModel: FileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SearchBar(
            searchQuery = state.searchQuery,
            onSearchQueryChanged = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Search global files..."
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(state.displayedFiles) { file ->
                FileCard(
                    fileName = file.name ?: "Unknown File",
                    fileSize = "${file.size}",
                    fileDate = file.date ?: "Unknown Date",
                    onDownloadClick = { },
                    onDeleteClick = { }
                )
            }
        }
    }
}