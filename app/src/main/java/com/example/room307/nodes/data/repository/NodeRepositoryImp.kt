package com.example.room307.nodes.data.repository

import com.example.room307.di.NodeUrlManager
import com.example.room307.nodes.data.remote.NodeApi
import com.example.room307.nodes.data.remote.NodeDto
import com.example.room307.nodes.domain.repository.NodeRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NodeRepositoryImp @Inject constructor(
    private val api: NodeApi,
    private val nodeUrlManager: NodeUrlManager,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) : NodeRepository {
    override suspend fun getAllNodes(): Result<List<NodeDto>> {
        return try {
            val response = api.getAllNodes()

            if (response.isSuccessful && response.body() != null) {
                val nodes = response.body()!!
                val urls = nodes.map { "http://${it.ip}:${it.port}/" }
                nodeUrlManager.updateDiscovered(urls)
                Result.success(nodes)
            } else {
                Result.failure(Exception("Failed to fetch nodes: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(ip: String, port: String): Result<Int> {
        val targetIp = if (ip == "localhost" || ip == "127.0.0.1") "10.0.2.2" else ip
        val baseUrl = "http://$targetIp:$port/"

        return try {
            val cleanClient = okHttpClient.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val contentType = "application/json".toMediaType()
            val tempApi = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(cleanClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(NodeApi::class.java)

            val response = tempApi.getAllNodes()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.size)
            } else {
                Result.failure(Exception("HTTP Error: ${response.code()}"))
            }
        } catch (e: ConnectException) {
            Result.failure(Exception("Connection refused. Is the server running?"))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Timeout. Check your IP and Firewall."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Connection failed"))
        }
    }
}
