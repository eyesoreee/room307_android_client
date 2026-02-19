package com.example.room307.settings.presentation

import android.net.Uri

sealed interface SettingsAction {
    data object ClearCache : SettingsAction
    data object UpdateCacheSize : SettingsAction
    data class SetDownloadPath(val uri: Uri?) : SettingsAction
    data class AddServerConfig(val ip: String, val port: String) : SettingsAction
    data class UpdateServerConfig(val id: Long, val ip: String, val port: String) : SettingsAction
    data class DeleteServerConfig(val id: Long) : SettingsAction
}
