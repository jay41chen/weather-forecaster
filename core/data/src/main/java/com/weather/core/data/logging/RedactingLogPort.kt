package com.weather.core.data.logging

import com.weather.core.logging.LogPort

class RedactingLogPort(
    private val delegate: LogPort,
    private val patterns: List<Pair<Regex, String>>
) : LogPort {

    override fun d(message: String, extras: Map<String, Any?>) =
        delegate.d(redact(message), redactExtras(extras))

    override fun i(message: String, extras: Map<String, Any?>) =
        delegate.i(redact(message), redactExtras(extras))

    override fun w(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegate.w(redact(message), throwable, redactExtras(extras))

    override fun e(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegate.e(redact(message), throwable, redactExtras(extras))

    private fun redact(text: String): String =
        patterns.fold(text) { acc, (regex, mask) -> regex.replace(acc, mask) }

    private fun redactExtras(extras: Map<String, Any?>): Map<String, Any?> =
        if (extras.isEmpty()) extras
        else extras.mapValues { (_, v) -> if (v is String) redact(v) else v }
}
