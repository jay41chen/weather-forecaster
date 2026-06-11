package com.weather.core.logging

class CompositeLogPort(private val delegates: List<LogPort>) : LogPort {

    override fun d(message: String, extras: Map<String, Any?>) =
        delegates.forEach { it.d(message, extras) }

    override fun i(message: String, extras: Map<String, Any?>) =
        delegates.forEach { it.i(message, extras) }

    override fun w(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegates.forEach { it.w(message, throwable, extras) }

    override fun e(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegates.forEach { it.e(message, throwable, extras) }
}
