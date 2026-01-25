package com.example.room307.files.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.room307.files.data.remote.FileDto
import com.example.room307.files.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import javax.inject.Inject

data class FileState(
    val files: List<FileDto> = emptyList(),
    val displayedFiles: List<FileDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class FileViewModel @Inject constructor(
    private val repository: FileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(FileState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fetchFiles()
        }
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }

            val file = uriToFile(uri)

            if (file != null) {
                repository.upload(file)
                    .onSuccess {
                        fetchFiles()
                        _state.update {
                            it.copy(
                                isUploading = false,
                                successMessage = "Upload Complete"
                            )
                        }
                    }
                    .onFailure { e ->
                        _state.update {
                            it.copy(
                                isUploading = false,
                                errorMessage = "Upload failed: ${e.message}"
                            )
                        }
                    }
            } else {
                _state.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = "Could not process file"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _state.update {
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onSearch(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                displayedFiles = filterFiles(it.files, query)
            )
        }
    }

    private fun fetchFiles() {
        viewModelScope.launch {
            if (_state.value.files.isEmpty())
                _state.update { it.copy(isLoading = true) }

            repository.getAll()
                .onSuccess { remoteFiles ->
                    _state.update {
                        it.copy(
                            files = remoteFiles,
                            displayedFiles = filterFiles(remoteFiles, it.searchQuery),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to fetch files: ${e.message}"
                        )
                    }
                }
        }
    }

    // ------------- HELPER FUNCTIONS -------------
    private fun filterFiles(files: List<FileDto>, query: String): List<FileDto> {
        if (query.isBlank())
            return files
        return files.filter { it.name?.contains(query, ignoreCase = true) ?: false }
    }

    private fun uriToFile(uri: Uri): java.io.File? {
        return try {
            val contentResolver = context.contentResolver

            var fileName = "temp_file"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        fileName = cursor.getString(index)
                    }
                }
            }

            val tempFile = java.io.File(context.cacheDir, fileName)

            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}