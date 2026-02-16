package com.example.room307.di

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class FailoverInterceptor : Interceptor {
    companion object {
        private val nodeUrls = listOf(
            "http://192.168.1.189:8001/",
            "http://192.168.1.189:8002/",
            "http://192.168.1.189:8003/"
        )
    }

    private val lastGoodIndex = AtomicInteger(0)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        val startFrom = lastGoodIndex.get()
        for (i in nodeUrls.indices) {
            val current = (startFrom + i) % nodeUrls.size
            val url = nodeUrls[current]

            try {
                val newBaseUrl = url.toHttpUrlOrNull() ?: continue
                val newUrl = request.url.newBuilder()
                    .scheme(newBaseUrl.scheme)
                    .host(newBaseUrl.host)
                    .port(newBaseUrl.port)
                    .build()

                val newRequest = request.newBuilder()
                    .url(newUrl)
                    .build()
                val response = chain.proceed(newRequest)
                lastGoodIndex.set(current)
                return response
            } catch (e: IOException) {
                lastException = e
                continue
            }
        }

        throw lastException ?: IOException("All nodes are unreachable")
    }
}