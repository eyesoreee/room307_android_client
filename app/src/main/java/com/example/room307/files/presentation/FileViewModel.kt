package com.example.room307.files.presentation

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.room307.files.data.remote.FileDto
import com.example.room307.files.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class FileUiState(
    val files: List<FileDto> = emptyList(),
    val displayedFiles: List<FileDto> = emptyList(),
    val searchQuery: String = "",
    val uploadProgress: Float? = null,
    val isRefreshing: Boolean = false,
)

sealed interface FileState {
    object Idle : FileState
    object Loading : FileState
    data class Error(val message: String) : FileState
    data class Ready(val uiState: FileUiState) : FileState
}

@HiltViewModel
class FileViewModel @Inject constructor(
    private val repository: FileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<FileState>(FileState.Idle)
    val state = _state.asStateFlow()

    init {
        loadFiles()
    }

    fun onAction(action: FileAction) {
        when (action) {
            is FileAction.LoadFiles -> {
                loadFiles()
            }

            is FileAction.SearchFiles -> {
                searchFiles(action.query)
            }

            is FileAction.DownloadFile -> {
                downloadFile(action.fileId)
            }

            is FileAction.DeleteFile -> {
                deleteFile(action.fileId)
            }

            is FileAction.UploadFile -> {
                uploadFile(action.file)
            }

            is FileAction.RefreshFiles -> {
                refreshFiles()
            }
        }
    }

    private fun refreshFiles() {
        val currentState = _state.value
        if (currentState !is FileState.Ready) return

        _state.update { state ->
            if (state is FileState.Ready) {
                state.copy(uiState = state.uiState.copy(isRefreshing = true))
            } else state
        }

        viewModelScope.launch {
            try {
                val result = repository.getAll().getOrThrow()
                _state.update { state ->
                    if (state is FileState.Ready) {
                        val query = state.uiState.searchQuery
                        val filtered = if (query.isBlank()) {
                            result
                        } else {
                            result.filter { it.name?.contains(query, ignoreCase = true) == true }
                        }
                        state.copy(
                            uiState = state.uiState.copy(
                                files = result,
                                displayedFiles = filtered,
                                isRefreshing = false
                            )
                        )
                    } else state
                }
            } catch (e: Exception) {
                _state.update { state ->
                    if (state is FileState.Ready) {
                        state.copy(uiState = state.uiState.copy(isRefreshing = false))
                    } else state
                }
            }
        }
    }

    private fun loadFiles() {
        _state.update { FileState.Loading }
        viewModelScope.launch {
            try {
                val files = repository.getAll().getOrThrow()
                _state.update {
                    FileState.Ready(
                        uiState = FileUiState(
                            files = files,
                            displayedFiles = files,
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { FileState.Error(e.message ?: "Unknown error") }
            }
        }
    }

    private fun uploadFile(file: File) {
        _state.update { FileState.Loading }
        viewModelScope.launch {
            try {
                repository.upload(file).getOrThrow()
                loadFiles()
            } catch (e: Exception) {
                _state.update { FileState.Error(e.message ?: "Upload failed") }
            }
        }
    }

    private fun deleteFile(fileId: String) {
        _state.update { FileState.Loading }
        viewModelScope.launch {
            try {
                repository.delete(fileId).getOrThrow()
                loadFiles()
            } catch (e: Exception) {
                _state.update { FileState.Error(e.message ?: "Delete failed") }
            }
        }
    }

    private fun downloadFile(fileId: String) {
        val currentState = _state.value
        if (currentState !is FileState.Ready) return

        val fileDto = currentState.uiState.files.find { it.id == fileId } ?: return
        val fileName = fileDto.name ?: "downloaded_file_${System.currentTimeMillis()}"

        _state.update { FileState.Loading }
        viewModelScope.launch {
            try {
                val responseBody = repository.download(fileId).getOrThrow()

                withContext(Dispatchers.IO) {
                    val downloadsFolder =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetFile = File(downloadsFolder, fileName)

                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                loadFiles()
            } catch (e: Exception) {
                _state.update { FileState.Error(e.message ?: "Download failed") }
            }
        }
    }

    private fun searchFiles(query: String) {
        _state.update { current ->
            when (current) {
                is FileState.Ready -> {
                    val filtered = if (query.isBlank()) {
                        current.uiState.files
                    } else {
                        current.uiState.files.filter { file ->
                            file.name?.contains(query, ignoreCase = true) == true
                        }
                    }

                    current.copy(
                        uiState = current.uiState.copy(
                            searchQuery = query,
                            displayedFiles = filtered,
                        )
                    )
                }

                else -> current
            }
        }
    }
}
