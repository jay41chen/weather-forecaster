package com.weather.core.logging

import timber.log.Timber

class TimberLogAdapter(private val tag: String) : LogPort {
    override fun d(message: String, extras: Map<String, Any?>) {
        Timber.tag(tag).d(formatMessage(message, extras))
    }
    override fun i(message: String, extras: Map<String, Any?>) {
        Timber.tag(tag).i(formatMessage(message, extras))
    }
    override fun w(message: String, throwable: Throwable?, extras: Map<String, Any?>) {
        Timber.tag(tag).w(throwable, formatMessage(message, extras))
    }
    override fun e(message: String, throwable: Throwable?, extras: Map<String, Any?>) {
        Timber.tag(tag).e(throwable, formatMessage(message, extras))
    }

    private fun formatMessage(message: String, extras: Map<String, Any?>): String {
        if (extras.isEmpty()) return message
        return "$message [${extras.entries.joinToString(", ") { "${it.key}=${it.value}" }}]"
    }
}
