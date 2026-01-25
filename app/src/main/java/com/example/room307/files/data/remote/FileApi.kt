package com.example.room307.files.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface FileApi {
    @GET("/api/v1/files")
    suspend fun getFiles(): Response<List<FileDto>>

    @Multipart
    @POST("/api/v1/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @Streaming
    @GET("/api/v1/download/{file_id}")
    suspend fun downloadFile(@Path("file_id") fileId: String): Response<ResponseBody>

    @POST("/api/v1/delete/{file_id}")
    suspend fun deleteFile(@Path("file_id") fileId: String): Response<Unit>
}