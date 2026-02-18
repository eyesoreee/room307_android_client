package com.example.room307.files.presentation

sealed interface FileEvent {
    data class ShowSnackbar(val message: String) : FileEvent
}