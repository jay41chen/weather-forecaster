package com.weather.core.data.logging

import com.weather.core.logging.LogPort
import okhttp3.logging.HttpLoggingInterceptor

class LogPortHttpLogger(private val log: LogPort) : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        log.d(message)
    }
}
