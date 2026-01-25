package com.example.room307.files.data.repository

import com.example.room307.files.data.remote.FileApi
import com.example.room307.files.data.remote.FileDto
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
    override suspend fun getAll(): Result<List<FileDto>> {
        return try {
            val response = api.getFiles()

            if (response.isSuccessful && response.body() != null)
                Result.success(response.body()!!)
            else
                Result.failure(Exception("Failed to fetch files"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upload(file: File): Result<Unit> {
        return try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = api.uploadFile(body)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upload failed code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun download(fileId: String): Result<ResponseBody> {
        return try {
            val response = api.downloadFile(fileId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Download failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(fileId: String): Result<Unit> {
        return try {
            val response = api.deleteFile(fileId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}