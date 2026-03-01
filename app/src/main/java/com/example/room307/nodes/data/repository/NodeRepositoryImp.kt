package com.example.room307.nodes.data.repository

import com.example.room307.di.NodeUrlManager
import com.example.room307.nodes.data.remote.NodeApi
import com.example.room307.nodes.data.remote.toNodeItem
import com.example.room307.nodes.domain.model.NodeItem
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
        val targetIp = if (ip == "localhost" || ip == "127.0.0.1") "10.0.2.2" else ip
        val baseUrl = "http://$targetIp:$port/"

        return runCatching {
            val cleanClient = OkHttpClient.Builder()
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
                response.body()!!.size
            } else {
                throw Exception("HTTP Error: ${response.code()}")
            }
        }.recoverCatching { e ->
            throw when (e) {
                is ConnectException -> Exception("Connection refused. Is the server running?")
                is SocketTimeoutException -> Exception("Timeout. Check your IP and Firewall.")
                else -> {
                    val message = if (e.message?.contains("No server nodes available") == true) {
                        "Internal error: Interceptor blocked the test."
                    } else {
                        e.localizedMessage ?: "Connection failed"
                    }
                    Exception(message)
                }
            }
        }
    }
}
