package com.example.room307.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadPathDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var path by remember { mutableStateOf(currentPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Subfolder") },
        text = {
            Column {
                Text(
                    text = "Files will be saved in: Downloads/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = path.ifEmpty { "(No subfolder)" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Subfolder Name") },
                    placeholder = { Text("e.g. ROOM307") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(path.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
