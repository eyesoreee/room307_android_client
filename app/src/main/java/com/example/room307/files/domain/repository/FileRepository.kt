package com.example.room307.files.domain.repository

import com.example.room307.files.data.remote.FileDto
import okhttp3.ResponseBody
import java.io.File

interface FileRepository {
    suspend fun getAll(): Result<List<FileDto>>
    suspend fun download(fileId: String): Result<ResponseBody>
    suspend fun upload(file: File): Result<Unit>
    suspend fun delete(fileId: String): Result<Unit>
}