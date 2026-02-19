package com.example.room307.settings.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val cacheSize: String = "0.0 B"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        updateCacheSize()
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().cacheDir.apply {
                deleteRecursively()
                mkdirs()
            }
            val size = getFolderSize(getApplication<Application>().cacheDir)
            _state.update { it.copy(cacheSize = formatSize(size)) }
        }
    }

    fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = getFolderSize(getApplication<Application>().cacheDir)
            _state.update { it.copy(cacheSize = formatSize(size)) }
        }
    }

    private fun getFolderSize(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun formatSize(size: Long): String = when {
        size < 1_024 -> "$size B"
        size < 1_048_576 -> "%.1f KB".format(size / 1_024.0)
        else -> "%.1f MB".format(size / 1_048_576.0)
    }
}
