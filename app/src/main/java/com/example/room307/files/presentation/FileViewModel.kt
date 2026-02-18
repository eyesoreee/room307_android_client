package com.example.room307.files.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.room307.files.domain.FileDownloader
import com.example.room307.files.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileViewModel @Inject constructor(
    private val repository: FileRepository,
    private val fileDownloader: FileDownloader
) : ViewModel() {

    private val _state = MutableStateFlow<FileState>(FileState.Idle)
    val state = _state.asStateFlow()

    private val _events = Channel<FileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadFiles()
    }

    fun onAction(action: FileAction) {
        when (action) {
            is FileAction.LoadFiles -> loadFiles()
            is FileAction.SearchFiles -> searchFiles(action.query)
            is FileAction.DownloadFile -> downloadFile(action.fileId, action.fileName)
            is FileAction.DeleteFile -> deleteFile(action.fileId)
            is FileAction.UploadFile -> uploadFile(action.file)
            is FileAction.RefreshFiles -> refreshFiles()
        }
    }

    private fun refreshFiles() {
        updateReadyState { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            repository.getAll()
                .onSuccess { result ->
                    updateReadyState { state ->
                        val filtered = if (state.searchQuery.isBlank()) result
                        else result.filter {
                            it.name?.contains(
                                state.searchQuery,
                                ignoreCase = true
                            ) == true
                        }
                        state.copy(files = result, displayedFiles = filtered, isRefreshing = false)
                    }
                }
                .onFailure { e ->
                    updateReadyState { it.copy(isRefreshing = false) }
                    _events.trySend(FileEvent.ShowSnackbar(e.message ?: "Refresh failed"))
                }
        }
    }

    private fun loadFiles() {
        _state.update { FileState.Loading }
        viewModelScope.launch {
            repository.getAll()
                .onSuccess { files ->
                    _state.update {
                        FileState.Ready(FileUiState(files = files, displayedFiles = files))
                    }
                }
                .onFailure { e ->
                    _state.update { FileState.Error(e.message ?: "Failed to load files") }
                }
        }
    }

    private fun uploadFile(file: File) {
        updateReadyState { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            repository.upload(file)
                .onSuccess {
                    _events.trySend(FileEvent.ShowSnackbar("Upload successful"))
                    refreshFiles()
                }
                .onFailure { e ->
                    updateReadyState { it.copy(isRefreshing = false) }
                    _events.trySend(FileEvent.ShowSnackbar(e.message ?: "Upload failed"))
                }
        }
    }

    private fun deleteFile(fileId: String) {
        updateReadyState { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            repository.delete(fileId)
                .onSuccess {
                    _events.trySend(FileEvent.ShowSnackbar("File deleted"))
                    refreshFiles()
                }
                .onFailure { e ->
                    updateReadyState { it.copy(isRefreshing = false) }
                    _events.trySend(FileEvent.ShowSnackbar(e.message ?: "Delete failed"))
                }
        }
    }

    private fun downloadFile(fileId: String, filename: String) {
        updateReadyState { it.copy(isDownloading = true) }
        viewModelScope.launch {
            repository.download(fileId)
                .onSuccess { body ->
                    viewModelScope.launch {
                        val success = fileDownloader.saveFileToDisk(filename, body)
                        updateReadyState { it.copy(isDownloading = false) }
                        if (!success) {
                            _events.trySend(FileEvent.ShowSnackbar("Failed to save file"))
                        } else {
                            _events.trySend(FileEvent.ShowSnackbar("File downloaded"))
                        }
                    }
                }
                .onFailure { e ->
                    updateReadyState { it.copy(isDownloading = false) }
                    _events.trySend(FileEvent.ShowSnackbar(e.message ?: "Download failed"))
                }
        }
    }

    private fun searchFiles(query: String) {
        updateReadyState { current ->
            val filtered = if (query.isBlank()) current.files
            else current.files.filter { it.name?.contains(query, ignoreCase = true) == true }
            current.copy(searchQuery = query, displayedFiles = filtered)
        }
    }

    private fun updateReadyState(transform: (FileUiState) -> FileUiState) {
        _state.update { current ->
            if (current is FileState.Ready) {
                FileState.Ready(transform(current.uiState))
            } else current
        }
    }
}
