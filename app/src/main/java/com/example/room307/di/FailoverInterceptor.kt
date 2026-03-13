package com.example.room307.di

import android.util.Log
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
        val urlsToTry = nodeUrlManager.urlsToTry.value

        if (urlsToTry.isEmpty()) {
            throw IOException("No server nodes available. Please configure a bootstrap node in settings.")
        }

        val startIndex = lastGoodIndex.get().coerceIn(0, urlsToTry.lastIndex)
        var lastException: IOException? = null

        for (i in urlsToTry.indices) {
            val currentIndex = (startIndex + i) % urlsToTry.size
            val nodeBaseUrl = urlsToTry[currentIndex]

            val newRequest = buildRequestForNode(chain.request(), nodeBaseUrl)
            if (newRequest == null) {
                Log.w(TAG, "Skipping malformed node URL at index $currentIndex: '$nodeBaseUrl'")
                continue
            }

            try {
                val response = chain.proceed(newRequest)

                if (response.isSuccessful) {
                    lastGoodIndex.set(currentIndex)
                    return response
                }

                Log.w(TAG, "Node $nodeBaseUrl returned ${response.code} — trying next.")
                response.close()
                lastException = IOException("Node $nodeBaseUrl returned HTTP ${response.code}")
            } catch (e: IOException) {
                Log.w(TAG, "Node $nodeBaseUrl unreachable: ${e.message}")
                lastException = e
            }
        }

        throw lastException ?: IOException("All configured nodes are unreachable.")
    }

    private fun buildRequestForNode(request: Request, baseUrl: String): Request? {
        val newBase = baseUrl.toHttpUrlOrNull() ?: return null

        val newUrl = request.url.newBuilder()
            .scheme(newBase.scheme)
            .host(newBase.host)
            .port(newBase.port)
            .build()

        return request.newBuilder().url(newUrl).build()
    }

    private companion object {
        const val TAG = "FailoverInterceptor"
    }
}