package com.example.room307.settings.presentation

import android.net.Uri

sealed interface SettingsAction {
    data object ClearCache : SettingsAction
    data object UpdateCacheSize : SettingsAction
    data class SetDownloadPath(val uri: Uri?) : SettingsAction
    data class SetBootstrapConfig(val ip: String, val port: String) : SettingsAction
    data class TestConnection(val ip: String, val port: String) : SettingsAction
    data object ResetTestResult : SettingsAction
    data class UpdateSyncFrequency(val minutes: Int) : SettingsAction
}
