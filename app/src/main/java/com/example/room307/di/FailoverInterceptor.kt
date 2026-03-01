package com.example.room307.di

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
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
        val originalRequest = chain.request()
        val urlsToTry = nodeUrlManager.urlsToTry.value

        if (urlsToTry.isEmpty()) {
            throw IOException("No server nodes available. Please configure a bootstrap node in settings.")
        }

        var lastException: IOException? = null
        val startIndex = lastGoodIndex.get()

        for (i in urlsToTry.indices) {
            val currentIndex = (startIndex + i) % urlsToTry.size
            val nodeBaseUrl = urlsToTry[currentIndex]

            try {
                val newRequest = buildRequestForNode(originalRequest, nodeBaseUrl)
                    ?: continue

                val response = chain.proceed(newRequest)

                lastGoodIndex.set(currentIndex)
                return response
            } catch (e: IOException) {
                lastException = e
            }
        }

        throw lastException ?: IOException("All configured nodes are unreachable.")
    }

    private fun buildRequestForNode(request: Request, baseUrl: String): Request? {
        val newBaseHttpUrl = baseUrl.toHttpUrlOrNull() ?: return null

        val newUrl = request.url.newBuilder()
            .scheme(newBaseHttpUrl.scheme)
            .host(newBaseHttpUrl.host)
            .port(newBaseHttpUrl.port)
            .build()

        return request.newBuilder()
            .url(newUrl)
            .build()
    }
}
