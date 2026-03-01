package com.example.room307.files.presentation

import com.example.room307.files.domain.model.FileItem

data class FileUiState(
    val files: List<FileItem> = emptyList(),
    val displayedFiles: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val uploadProgress: Float? = null,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false
)
