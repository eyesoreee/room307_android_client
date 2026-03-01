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
            FileAction.LoadFiles -> loadFiles()
            is FileAction.SearchFiles -> searchFiles(action.query)
            is FileAction.DownloadFile -> downloadFile(action.fileId, action.fileName)
            is FileAction.DeleteFile -> deleteFile(action.fileId)
            is FileAction.UploadFile -> uploadFile(action.file)
            FileAction.RefreshFiles -> refreshFiles()
        }
    }

    private fun loadFiles(isRefreshing: Boolean = false) {
        if (!isRefreshing) _state.update { FileState.Loading }
        else updateReadyState { it.copy(isRefreshing = true) }

        viewModelScope.launch {
            repository.getAll()
                .onSuccess { files ->
                    _state.update { current ->
                        val searchQuery = (current as? FileState.Ready)?.uiState?.searchQuery ?: ""
                        val filtered = if (searchQuery.isBlank()) files
                        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        
                        FileState.Ready(FileUiState(
                            files = files,
                            displayedFiles = filtered,
                            searchQuery = searchQuery,
                            isRefreshing = false
                        ))
                    }
                }
                .onFailure { e ->
                    if (isRefreshing) {
                        updateReadyState { it.copy(isRefreshing = false) }
                        _events.trySend(FileEvent.ShowSnackbar(e.message ?: "Refresh failed"))
                    } else {
                        _state.update { FileState.Error(e.message ?: "Failed to load files") }
                    }
                }
        }
    }

    private fun refreshFiles() = loadFiles(isRefreshing = true)

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
                    val success = fileDownloader.saveFileToDisk(filename, body)
                    updateReadyState { it.copy(isDownloading = false) }
                    val message = if (success) "File downloaded" else "Failed to save file"
                    _events.trySend(FileEvent.ShowSnackbar(message))
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
            else current.files.filter { it.name.contains(query, ignoreCase = true) }
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
