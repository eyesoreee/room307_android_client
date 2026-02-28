package com.example.room307.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    onThemeChange: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showBootstrapDialog by remember { mutableStateOf(false) }
    var showSyncFrequencyDialog by remember { mutableStateOf(false) }
    var showDownloadPathDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsGroup(title = "Connection") {
            SettingsItem(
                icon = Icons.Default.CloudQueue,
                title = "Server Configuration",
                subtitle = "${state.initialServerConfig.ip}:${state.initialServerConfig.port}",
                onClick = { showBootstrapDialog = true }
            )
            
            val frequencyText = if (state.syncFrequency == 60) "Every hour" else "Every ${state.syncFrequency} minutes"
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Node Sync Frequency",
                subtitle = frequencyText,
                onClick = { showSyncFrequencyDialog = true }
            )
        }

        SettingsGroup(title = "Storage") {
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "Clear App Cache",
                subtitle = "Currently using ${state.cacheSize}",
                onClick = { viewModel.onAction(SettingsAction.ClearCache) }
            )
            SettingsItem(
                icon = Icons.Default.Storage,
                title = "Default Download Path",
                subtitle = "Downloads/${state.downloadPath}",
                onClick = { showDownloadPathDialog = true }
            )
        }

        SettingsGroup(title = "Appearance") {
            SettingsToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "Use dark theme for the app",
                checked = isDarkTheme,
                onCheckedChange = { onThemeChange() }
            )
            SettingsToggleItem(
                icon = Icons.Default.Palette,
                title = "Dynamic Colors",
                subtitle = "Sync with system wallpaper colors",
                checked = state.dynamicColors,
                onCheckedChange = { enabled -> 
                    viewModel.onAction(SettingsAction.ToggleDynamicColors(enabled))
                }
            )
        }

        SettingsGroup(title = "About") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.0.0-alpha (Build 2024.11)",
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showBootstrapDialog) {
        BootstrapNodeDialog(
            config = state.initialServerConfig,
            testResult = state.testResult,
            onTest = { ip, port -> viewModel.onAction(SettingsAction.TestConnection(ip, port)) },
            onResetTest = { viewModel.onAction(SettingsAction.ResetTestResult) },
            onDismiss = { showBootstrapDialog = false },
            onConfirm = { ip, port ->
                viewModel.onAction(SettingsAction.SetBootstrapConfig(ip, port))
                showBootstrapDialog = false
            }
        )
    }

    if (showSyncFrequencyDialog) {
        SyncFrequencyDialog(
            currentFrequency = state.syncFrequency,
            onDismiss = { showSyncFrequencyDialog = false },
            onConfirm = { minutes ->
                viewModel.onAction(SettingsAction.UpdateSyncFrequency(minutes))
                showSyncFrequencyDialog = false
            }
        )
    }

    if (showDownloadPathDialog) {
        DownloadPathDialog(
            currentPath = state.downloadPath,
            onDismiss = { showDownloadPathDialog = false },
            onConfirm = { path ->
                viewModel.onAction(SettingsAction.SetDownloadPath(path))
                showDownloadPathDialog = false
            }
        )
    }
}
