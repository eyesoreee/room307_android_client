package com.example.room307.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "bootstrap_prefs")

private val BOOTSTRAP_KEY = stringPreferencesKey("bootstrap_config")
private val SYNC_FREQUENCY_KEY = intPreferencesKey("sync_frequency")
private val DOWNLOAD_PATH_KEY = stringPreferencesKey("download_path")
private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val serverAddress: Flow<String?> = context.dataStore.data
        .map { it[BOOTSTRAP_KEY] }

    val syncFrequency: Flow<Int> = context.dataStore.data
        .map { it[SYNC_FREQUENCY_KEY] ?: DEFAULT_SYNC_FREQUENCY_MINUTES }

    val downloadPath: Flow<String?> = context.dataStore.data
        .map { it[DOWNLOAD_PATH_KEY] }

    val dynamicColors: Flow<Boolean> = context.dataStore.data
        .map { it[DYNAMIC_COLORS_KEY] ?: true }

    suspend fun updateServerAddress(address: String) {
        context.dataStore.edit { it[BOOTSTRAP_KEY] = address }
    }

    suspend fun updateSyncFrequency(minutes: Int) {
        require(minutes > 0) { "Sync frequency must be positive, got $minutes" }
        context.dataStore.edit { it[SYNC_FREQUENCY_KEY] = minutes }
    }

    suspend fun updateDownloadPath(path: String) {
        context.dataStore.edit { it[DOWNLOAD_PATH_KEY] = path }
    }

    suspend fun updateDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLORS_KEY] = enabled }
    }

    companion object {
        const val DEFAULT_SYNC_FREQUENCY_MINUTES = 5
    }
}