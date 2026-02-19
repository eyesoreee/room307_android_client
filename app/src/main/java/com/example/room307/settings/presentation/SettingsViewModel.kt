package com.example.room307.settings.presentation

import android.app.Application
import android.net.Uri
import android.os.Environment
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

data class ServerConfig(
    val id: Long = System.currentTimeMillis(),
    val ip: String,
    val port: String
)

data class SettingsUiState(
    val cacheSize: String = "0.0 B",
    val downloadPath: String = "",
    val serverConfigs: List<ServerConfig> = listOf(
        ServerConfig(ip = "192.168.1.189", port = "8001")
    )
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        updateCacheSize()
        val defaultPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        _state.update { it.copy(downloadPath = defaultPath) }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.ClearCache -> clearCache()
            SettingsAction.UpdateCacheSize -> updateCacheSize()
            is SettingsAction.SetDownloadPath -> setDownloadPath(action.uri)
            is SettingsAction.AddServerConfig -> addServerConfig(action.ip, action.port)
            is SettingsAction.UpdateServerConfig -> updateServerConfig(action.id, action.ip, action.port)
            is SettingsAction.DeleteServerConfig -> deleteServerConfig(action.id)
        }
    }

    private fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().cacheDir.apply {
                deleteRecursively()
                mkdirs()
            }
            updateCacheSizeInternal()
        }
    }

    private fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            updateCacheSizeInternal()
        }
    }

    private suspend fun updateCacheSizeInternal() {
        val size = getFolderSize(getApplication<Application>().cacheDir)
        _state.update { it.copy(cacheSize = formatSize(size)) }
    }

    private fun setDownloadPath(uri: Uri?) {
        uri?.let {
            _state.update { it.copy(downloadPath = uri.toString()) }
        }
    }

    private fun addServerConfig(ip: String, port: String) {
        _state.update { currentState ->
            currentState.copy(
                serverConfigs = currentState.serverConfigs + ServerConfig(ip = ip, port = port)
            )
        }
    }

    private fun updateServerConfig(id: Long, ip: String, port: String) {
        _state.update { currentState ->
            currentState.copy(
                serverConfigs = currentState.serverConfigs.map {
                    if (it.id == id) it.copy(ip = ip, port = port) else it
                }
            )
        }
    }

    private fun deleteServerConfig(id: Long) {
        _state.update { currentState ->
            currentState.copy(
                serverConfigs = currentState.serverConfigs.filter { it.id != id }
            )
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
