package com.example.room307.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ServerConfigDialog(
    configs: List<ServerConfig>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onEdit: (Long, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<ServerConfig?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server Configurations") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(configs) { config ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(config.ip, style = MaterialTheme.typography.bodyLarge)
                                Text("Port: ${config.port}", style = MaterialTheme.typography.bodySmall)
                            }
                            Row {
                                IconButton(onClick = { showEditDialog = config }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { onDelete(config.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add New")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    if (showAddDialog) {
        EditServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { ip, port ->
                onAdd(ip, port)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { config ->
        EditServerDialog(
            initialIp = config.ip,
            initialPort = config.port,
            onDismiss = { showEditDialog = null },
            onConfirm = { ip, port ->
                onEdit(config.id, ip, port)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun EditServerDialog(
    initialIp: String = "",
    initialPort: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialIp.isEmpty()) "Add Server" else "Edit Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    placeholder = { Text("e.g. 192.168.1.1") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    placeholder = { Text("e.g. 8001") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(ip, port) },
                enabled = ip.isNotBlank() && port.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
