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
import kotlinx.coroutines.withContext
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
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
            updateCacheSize()
        }
    }

    fun updateCacheSize() {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                getFolderSize(getApplication<Application>().cacheDir)
            }
            _state.update { it.copy(cacheSize = formatSize(size)) }
        }
    }

    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                size += getFolderSize(it)
            }
        } else {
            size = file.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        val s = size.toDouble()
        return when {
            s < 1024 -> "$size B"
            s < 1024 * 1024 -> "%.1f KB".format(s / 1024)
            else -> "%.1f MB".format(s / (1024 * 1024))
        }
    }
}
