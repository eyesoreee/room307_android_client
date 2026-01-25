package com.example.room307

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import dagger.hilt.android.HiltAndroidApp

enum class Destination(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val route: Screen
) {
    FILES(
        label = "Files",
        icon = Icons.Default.Folder,
        contentDescription = "Files section",
        route = Screen.Files
    ),
    NODES(
        label = "Nodes",
        icon = Icons.Default.Hub,
        contentDescription = "Nodes section",
        route = Screen.Nodes
    ),
    SETTINGS(
        label = "Settings",
        icon = Icons.Default.Settings,
        contentDescription = "Settings section",
        route = Screen.Settings
    )
}

@HiltAndroidApp
class MyApplication : Application()