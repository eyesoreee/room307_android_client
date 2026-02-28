package com.example.room307.di

import com.example.room307.data.local.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeUrlManager @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var syncJob: Job? = null

    private val _discoveredUrls = MutableStateFlow<List<String>>(emptyList())

    val urlsToTry: StateFlow<List<String>> = combine(
        dataStoreManager.serverAddress,
        _discoveredUrls
    ) { bootstrap, discovered ->
        (discovered + bootstrap).filterNotNull().distinct()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun startSyncLoop(onSync: suspend () -> Unit) {
        dataStoreManager.syncFrequency
            .distinctUntilChanged()
            .onEach { minutes ->
                syncJob?.cancel()
                syncJob = scope.launch {
                    while (true) {
                        onSync()
                        delay(minutes * 60 * 1000L)
                    }
                }
            }
            .launchIn(scope)
    }

    fun updateDiscovered(urls: List<String>) {
        _discoveredUrls.value = urls
    }

    fun clearDiscovered() {
        _discoveredUrls.value = emptyList()
    }
}
