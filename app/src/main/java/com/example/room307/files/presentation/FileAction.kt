package com.example.room307.files.presentation

import java.io.File

sealed class FileAction {
    object LoadFiles : FileAction()
    object RefreshFiles : FileAction()
    data class SearchFiles(val query: String) : FileAction()
    data class DownloadFile(val fileId: String) : FileAction()
    data class DeleteFile(val fileId: String) : FileAction()
    data class UploadFile(val file: File) : FileAction()
}