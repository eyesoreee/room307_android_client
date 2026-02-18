package com.example.room307.files.presentation

sealed interface FileState {
    object Idle : FileState
    object Loading : FileState
    data class Error(val message: String) : FileState
    data class Ready(val uiState: FileUiState) : FileState
}