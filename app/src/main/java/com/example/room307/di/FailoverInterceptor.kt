package com.example.room307.di

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailoverInterceptor @Inject constructor(
    private val nodeUrlManager: NodeUrlManager
) : Interceptor {

    private val lastGoodIndex = AtomicInteger(0)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val urlsToTry = nodeUrlManager.urlsToTry.value

        if (urlsToTry.isEmpty()) {
            throw IOException("No server nodes available. Please configure in settings.")
        }

        var lastException: IOException? = null
        val startFrom = lastGoodIndex.get()

        for (i in urlsToTry.indices) {
            val current = (startFrom + i) % urlsToTry.size
            val url = urlsToTry[current]

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

        throw lastException ?: IOException("All configured nodes are unreachable.")
    }
}
