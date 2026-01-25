package com.example.room307.nodes.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.room307.ui.SearchBar

@Composable
fun NodeScreen(modifier: Modifier = Modifier) {
    val mockNodes = listOf(
        Node(
            id = "1",
            name = "Server 1",
            ip = "192.168.1.1",
            status = NodeStatus.ONLINE,
            storage = StorageInfo(45, 100),
            uptime = "15d 6h",
            latency = 12
        ),
        Node(
            id = "2",
            name = "Server 2",
            ip = "192.168.1.2",
            status = NodeStatus.WARNING,
            storage = StorageInfo(85, 100),
            uptime = "2d 14h",
            latency = 45
        ),
        Node(
            id = "3",
            name = "Server 3",
            ip = "192.168.1.3",
            status = NodeStatus.OFFLINE,
            storage = StorageInfo(0, 100),
            uptime = "0d 0h",
            latency = 0
        )
    )


    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SearchBar(
            searchQuery = "",
            onSearchQueryChanged = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Search nodes..."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatsCard(
                value = "9",
                label = "Total",
                modifier = Modifier.weight(1f),
                valueColor = MaterialTheme.colorScheme.primary
            )
            StatsCard(
                value = "3",
                label = "Online",
                modifier = Modifier.weight(1f),
                valueColor = Color(0xFF4CAF50),
            )
            StatsCard(
                value = "2",
                label = "Offline",
                modifier = Modifier.weight(1f),
                valueColor = Color(0xFFEF5350)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mockNodes) {
                NodeCard(node = it)
            }
        }
    }
}
