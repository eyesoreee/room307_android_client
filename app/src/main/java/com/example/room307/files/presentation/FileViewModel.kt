package com.example.room307.files.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.room307.files.data.remote.FileDto
import com.example.room307.files.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FileState(
    val files: List<FileDto> = emptyList(),
    val displayedFiles: List<FileDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
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
}