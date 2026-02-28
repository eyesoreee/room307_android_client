package com.example.room307.settings.presentation

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.room307.data.local.DataStoreManager
import com.example.room307.di.NodeUrlManager
import com.example.room307.nodes.domain.repository.NodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ServerConfig(
    val ip: String,
    val port: String
)

sealed interface TestResult {
    object Idle : TestResult
    object Testing : TestResult
    data class Success(val nodeCount: Int) : TestResult
    data class Error(val message: String) : TestResult
}

data class SettingsUiState(
    val cacheSize: String = "0.0 B",
    val downloadPath: String = "ROOM307",
    val initialServerConfig: ServerConfig = ServerConfig("192.168.1.189", "8001"),
    val syncFrequency: Int = 5,
    val dynamicColors: Boolean = true,
    val testResult: TestResult = TestResult.Idle
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val dataStoreManager: DataStoreManager,
    private val nodeRepository: NodeRepository,
    private val nodeUrlManager: NodeUrlManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        updateCacheSize()

        viewModelScope.launch {
            dataStoreManager.serverAddress.collect { address ->
                address?.let { url ->
                    val cleanUrl = url.removePrefix("http://").removePrefix("https://").removeSuffix("/")
                    val parts = cleanUrl.split(":")
                    if (parts.size >= 2) {
                        _state.update { it.copy(initialServerConfig = ServerConfig(parts[0], parts[1])) }
                    }
                }
            }
        }

        viewModelScope.launch {
            dataStoreManager.syncFrequency.collect { frequency ->
                _state.update { it.copy(syncFrequency = frequency) }
            }
        }

        viewModelScope.launch {
            dataStoreManager.downloadPath.collect { path ->
                _state.update { it.copy(downloadPath = path ?: "ROOM307") }
            }
        }

        viewModelScope.launch {
            dataStoreManager.dynamicColors.collect { enabled ->
                _state.update { it.copy(dynamicColors = enabled) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ClearCache -> clearCache()
            is SettingsAction.UpdateCacheSize -> updateCacheSize()
            is SettingsAction.SetDownloadPath -> setDownloadPath(action.path)
            is SettingsAction.SetBootstrapConfig -> updateServerConfig(action.ip, action.port)
            is SettingsAction.TestConnection -> testConnection(action.ip, action.port)
            is SettingsAction.ResetTestResult -> _state.update { it.copy(testResult = TestResult.Idle) }
            is SettingsAction.UpdateSyncFrequency -> updateSyncFrequency(action.minutes)
            is SettingsAction.ToggleDynamicColors -> toggleDynamicColors(action.enabled)
        }
    }

    private fun testConnection(ip: String, port: String) {
        viewModelScope.launch {
            _state.update { it.copy(testResult = TestResult.Testing) }
            nodeRepository.testConnection(ip, port)
                .onSuccess { count ->
                    _state.update { it.copy(testResult = TestResult.Success(count)) }
                }
                .onFailure { error ->
                    _state.update { it.copy(testResult = TestResult.Error(error.message ?: "Test failed")) }
                }
        }
    }

    private fun updateSyncFrequency(minutes: Int) {
        viewModelScope.launch {
            dataStoreManager.updateSyncFrequency(minutes)
        }
    }

    private fun toggleDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.updateDynamicColors(enabled)
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

    private fun updateCacheSizeInternal() {
        val size = getFolderSize(getApplication<Application>().cacheDir)
        _state.update { it.copy(cacheSize = formatSize(size)) }
    }

    private fun setDownloadPath(path: String) {
        viewModelScope.launch {
            dataStoreManager.updateDownloadPath(path)
        }
    }

    private fun updateServerConfig(ip: String, port: String) {
        viewModelScope.launch {
            nodeUrlManager.clearDiscovered()
            val address = "http://$ip:$port/"
            dataStoreManager.updateServerAddress(address)
            nodeRepository.getAllNodes()
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
