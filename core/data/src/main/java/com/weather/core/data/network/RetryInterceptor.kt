package com.weather.core.data.network

import com.weather.core.model.RateLimitException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= MAX_RETRIES) {
            try {
                val response = chain.proceed(request)
                when {
                    response.code == 429 -> {
                        val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
                        response.close()
                        throw RateLimitException(retryAfterSeconds)
                    }
                    response.code in 500..599 && attempt < MAX_RETRIES -> {
                        response.close()
                        sleepWithInterrupt(BACKOFF_DELAYS[attempt])
                        attempt++
                    }
                    else -> return response
                }
            } catch (e: IOException) {
                if (e is RateLimitException) throw e
                lastException = e
                if (attempt < MAX_RETRIES) sleepWithInterrupt(BACKOFF_DELAYS[attempt])
                attempt++
            }
        }
        throw lastException ?: IOException("Request failed after $MAX_RETRIES retries")
    }

    private fun sleepWithInterrupt(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted during retry backoff", e)
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        private val BACKOFF_DELAYS = longArrayOf(1_000L, 2_000L, 4_000L)
    }
}
