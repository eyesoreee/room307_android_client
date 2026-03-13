package com.example.room307.nodes.data.repository

import android.util.Log
import com.example.room307.di.NodeUrlManager
import com.example.room307.nodes.data.remote.NodeApi
import com.example.room307.nodes.data.remote.toNodeItem
import com.example.room307.nodes.domain.model.NodeItem
import com.example.room307.nodes.domain.repository.NodeRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NodeRepositoryImp @Inject constructor(
    private val api: NodeApi,
    private val nodeUrlManager: NodeUrlManager,
    private val json: Json,
) : NodeRepository {

    override suspend fun getAllNodes(): Result<List<NodeItem>> = runCatching {
        val response = api.getAllNodes()
        if (response.isSuccessful) {
            val nodes = response.body() ?: emptyList()
            val urls = nodes.map { "http://${it.ip}:${it.port}/" }
            nodeUrlManager.updateDiscovered(urls)
            nodes.map { it.toNodeItem() }
        } else {
            throw Exception("Failed to fetch nodes: ${response.code()}")
        }
    }

    override suspend fun testConnection(ip: String, port: String): Result<Int> {
        val trimmedIp = ip.trim()
        val trimmedPort = port.trim()

        val targetIp = when (trimmedIp) {
            "localhost", "127.0.0.1" -> "10.0.2.2"
            else -> trimmedIp
        }

        val baseUrl = "http://$targetIp:$trimmedPort/"
        Log.d("TestConnection", "Testing: $baseUrl")

        return runCatching {
            val cleanClient = OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "ROOM307-Android-Client")
                        .header("Connection", "close")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val contentType = "application/json".toMediaType()
            val tempApi = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(cleanClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(NodeApi::class.java)

            val response = tempApi.getAllNodes()
            if (response.isSuccessful) {
                val nodes = response.body() ?: emptyList()
                Log.d("TestConnection", "Success! Found ${nodes.size} nodes.")
                nodes.size
            } else {
                val errorMsg =
                    "HTTP ${response.code()}: ${response.errorBody()?.string() ?: "Unknown error"}"
                Log.e("TestConnection", errorMsg)
                throw Exception(errorMsg)
            }
        }.recoverCatching { e ->
            Log.e("TestConnection", "Failed: ${e.message}", e)
            throw when (e) {
                is ConnectException -> Exception("Connection refused. Is the server running on $baseUrl?")
                is SocketTimeoutException -> Exception("Timeout. Server took too long to respond (Backend might be slow pinging nodes).")
                else -> Exception("Connection failed: ${e.localizedMessage}")
            }
        }
    }
}
