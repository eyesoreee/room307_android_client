package com.example.room307

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.example.room307.di.NodeUrlManager
import com.example.room307.nodes.domain.repository.NodeRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var nodeRepository: NodeRepository

    @Inject
    lateinit var nodeUrlManager: NodeUrlManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nodeUrlManager.startSyncLoop {
            nodeRepository.getAllNodes()
        }

        enableEdgeToEdge()
        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }

            var isDarkTheme by remember { mutableStateOf(true) }

            AppRoot(
                isDarkTheme = isDarkTheme,
                onThemeChange = { isDarkTheme = !isDarkTheme }
            )
        }
    }
}
