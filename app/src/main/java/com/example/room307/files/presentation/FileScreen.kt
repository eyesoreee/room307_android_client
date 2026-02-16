package com.example.room307.files.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.room307.ui.SearchBar
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun FileScreen(
    modifier: Modifier = Modifier,
    viewModel: FileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fileToDeleteId by remember { mutableStateOf<String?>(null) }
    var fileToDownloadId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            if (file != null) {
                viewModel.onAction(FileAction.UploadFile(file))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            is FileState.Error -> {
                Text(
                    text = (state as FileState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is FileState.Loading -> {
                CircularProgressIndicator()
            }

            is FileState.Ready -> {
                val uiState = (state as FileState.Ready).uiState

                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.onAction(FileAction.RefreshFiles) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SearchBar(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChanged = { viewModel.onAction(FileAction.SearchFiles(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Search global files..."
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(uiState.displayedFiles) { file ->
                                FileCard(
                                    fileName = file.name ?: "Unknown File",
                                    fileSize = file.getFormattedSize(),
                                    fileDate = file.getFormattedDate(),
                                    onDownloadClick = {
                                        fileToDownloadId = file.id
                                    },
                                    onDeleteClick = {
                                        fileToDeleteId = file.id
                                    }
                                )
                            }
                        }
                    }
                }
            }

            else -> {}
        }

        FloatingActionButton(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add File"
            )
        }
    }

    fileToDeleteId?.let { fileId ->
        AlertDialog(
            onDismissRequest = { fileToDeleteId = null },
            title = { Text(text = "Delete File") },
            text = { Text(text = "Are you sure you want to delete this file? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(FileAction.DeleteFile(fileId))
                        fileToDeleteId = null
                    }
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDeleteId = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    fileToDownloadId?.let { fileId ->
        AlertDialog(
            onDismissRequest = { fileToDownloadId = null },
            title = { Text(text = "Download File") },
            text = { Text(text = "Are you sure you want to download this file?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(FileAction.DownloadFile(fileId))
                        fileToDownloadId = null
                    }
                ) {
                    Text(text = "Download", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDownloadId = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

private fun uriToFile(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver
    val fileName = getFileName(context, uri) ?: "temp_file"
    val file = File(context.cacheDir, fileName)

    return try {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file
    } catch (e: Exception) {
        null
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}
