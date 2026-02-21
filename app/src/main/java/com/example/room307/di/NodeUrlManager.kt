package com.example.room307.di

import com.example.room307.data.local.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeUrlManager @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _discoveredUrls = MutableStateFlow<List<String>>(emptyList())

    val urlsToTry: StateFlow<List<String>> = combine(
        dataStoreManager.serverAddress,
        _discoveredUrls
    ) { bootstrap, discovered ->
        (discovered + bootstrap).filterNotNull().distinct()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun updateDiscovered(urls: List<String>) {
        _discoveredUrls.value = urls
    }

    fun clearDiscovered() {
        _discoveredUrls.value = emptyList()
    }
}
