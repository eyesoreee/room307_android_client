package com.example.room307.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
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
    }

    val serverAddress: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[BOOTSTRAP_KEY] }

    suspend fun updateServerAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[BOOTSTRAP_KEY] = if (address.endsWith("/")) address else "$address/"
        }
    }
}
