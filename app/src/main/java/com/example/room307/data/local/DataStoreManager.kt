package com.example.room307.data.local

import android.content.Context
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

@Singleton
class DataStoreManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val BOOTSTRAP_KEY = stringPreferencesKey("bootstrap_config")
        private val SYNC_FREQUENCY_KEY = intPreferencesKey("sync_frequency")
        private val DOWNLOAD_PATH_KEY = stringPreferencesKey("download_path")
    }

    val serverAddress: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[BOOTSTRAP_KEY] }

    val syncFrequency: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[SYNC_FREQUENCY_KEY] ?: 5 }

    val downloadPath: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[DOWNLOAD_PATH_KEY] }

    suspend fun updateServerAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[BOOTSTRAP_KEY] = if (address.endsWith("/")) address else "$address/"
        }
    }

    suspend fun updateSyncFrequency(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_FREQUENCY_KEY] = minutes
        }
    }

    suspend fun updateDownloadPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_PATH_KEY] = path
        }
    }
}
