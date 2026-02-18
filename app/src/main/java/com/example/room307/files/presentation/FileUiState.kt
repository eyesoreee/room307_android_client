package com.example.room307.files.presentation

import com.example.room307.files.data.remote.FileDto

data class FileUiState(
    val files: List<FileDto> = emptyList(),
    val displayedFiles: List<FileDto> = emptyList(),
    val searchQuery: String = "",
    val uploadProgress: Float? = null,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false
)