package com.weather.core.logging

interface LogPort {
    fun d(message: String, extras: Map<String, Any?> = emptyMap())
    fun i(message: String, extras: Map<String, Any?> = emptyMap())
    fun w(message: String, throwable: Throwable? = null, extras: Map<String, Any?> = emptyMap())
    fun e(message: String, throwable: Throwable? = null, extras: Map<String, Any?> = emptyMap())
}
