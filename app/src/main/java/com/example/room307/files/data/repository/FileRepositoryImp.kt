package com.example.room307.files.data.repository

import com.example.room307.files.data.remote.FileApi
import com.example.room307.files.data.remote.toFileItem
import com.example.room307.files.domain.model.FileItem
import com.example.room307.files.domain.repository.FileRepository
import jakarta.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File

class FileRepositoryImp @Inject constructor(
    private val api: FileApi
) : FileRepository {

    override suspend fun getAll(): Result<List<FileItem>> = runCatching {
        val response = api.getFiles()
        if (response.isSuccessful) {
            response.body()?.map { it.toFileItem() } ?: emptyList()
        } else {
            throw Exception("Failed to fetch files: ${response.code()}")
        }
    }

    override suspend fun upload(file: File): Result<Unit> = runCatching {
        val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val response = api.uploadFile(body)
        if (!response.isSuccessful) {
            throw Exception("Upload failed: ${response.code()}")
        }
    }

    override suspend fun download(fileId: String): Result<ResponseBody> = runCatching {
        val response = api.downloadFile(fileId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            body
        } else {
            throw Exception("Download failed: ${response.code()}")
        }
    }

    override suspend fun delete(fileId: String): Result<Unit> = runCatching {
        val response = api.deleteFile(fileId)
        if (!response.isSuccessful) {
            throw Exception("Delete failed: ${response.code()}")
        }
    }
}
