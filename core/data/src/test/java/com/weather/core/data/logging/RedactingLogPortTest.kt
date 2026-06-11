package com.weather.core.data.logging

import com.weather.core.logging.LogPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RedactingLogPortTest {

    private val apiKeyPattern = Regex("[a-fA-F0-9]{32,}") to "***REDACTED***"

    @Test
    fun `redacts 32-char hex string in message`() {
        val recorder = RecordingLogPort()
        val redacting = RedactingLogPort(recorder, listOf(apiKeyPattern))

        redacting.d("url=https://api.example.com?appid=7d8e9cb5960968969fc7f4d6639b4288")

        assertEquals(
            "url=https://api.example.com?appid=***REDACTED***",
            recorder.lastMessage
        )
    }

    @Test
    fun `redacts hex string in extras values`() {
        val recorder = RecordingLogPort()
        val redacting = RedactingLogPort(recorder, listOf(apiKeyPattern))

        redacting.i("request", mapOf("key" to "7d8e9cb5960968969fc7f4d6639b4288"))

        assertEquals("***REDACTED***", recorder.lastExtras["key"])
    }

    @Test
    fun `non-matching message passes through unchanged`() {
        val recorder = RecordingLogPort()
        val redacting = RedactingLogPort(recorder, listOf(apiKeyPattern))

        redacting.d("normal log message", mapOf("count" to "5"))

        assertEquals("normal log message", recorder.lastMessage)
        assertEquals("5", recorder.lastExtras["count"])
    }

    @Test
    fun `empty extras short-circuits without allocation`() {
        val recorder = RecordingLogPort()
        val redacting = RedactingLogPort(recorder, listOf(apiKeyPattern))
        val empty = emptyMap<String, Any?>()

        redacting.d("msg", empty)

        assertSame(empty, recorder.lastExtras)
    }

    @Test
    fun `non-string extras values pass through unmodified`() {
        val recorder = RecordingLogPort()
        val redacting = RedactingLogPort(recorder, listOf(apiKeyPattern))

        redacting.d("msg", mapOf("count" to 42, "flag" to true))

        assertEquals(42, recorder.lastExtras["count"])
        assertEquals(true, recorder.lastExtras["flag"])
    }

    @Test
    fun `throwable passes through unmodified on w`() {
        val recorder = RecordingLogPort()
        val redacting = RedactingLogPort(recorder, listOf(apiKeyPattern))
        val error = RuntimeException("boom")

        redacting.w("warning with 7d8e9cb5960968969fc7f4d6639b4288", error)

        assertSame(error, recorder.lastThrowable)
        assertEquals("warning with ***REDACTED***", recorder.lastMessage)
    }

    @Test
    fun `throwable passes through unmodified on e`() {
        val recorder = RecordingLogPort()
        val redacting = RedactingLogPort(recorder, listOf(apiKeyPattern))
        val error = RuntimeException("fatal")

        redacting.e("error with 7d8e9cb5960968969fc7f4d6639b4288", error)

        assertSame(error, recorder.lastThrowable)
        assertEquals("error with ***REDACTED***", recorder.lastMessage)
    }

    @Test
    fun `multiple patterns applied in order`() {
        val recorder = RecordingLogPort()
        val patterns = listOf(
            apiKeyPattern,
            Regex("Bearer\\s+\\S+") to "Bearer ***"
        )
        val redacting = RedactingLogPort(recorder, patterns)

        redacting.d("auth=Bearer token123 key=7d8e9cb5960968969fc7f4d6639b4288")

        assertEquals("auth=Bearer *** key=***REDACTED***", recorder.lastMessage)
    }

    // --- helpers ---

    private class RecordingLogPort : LogPort {
        var lastMessage: String = ""
        var lastThrowable: Throwable? = null
        var lastExtras: Map<String, Any?> = emptyMap()

        override fun d(message: String, extras: Map<String, Any?>) {
            lastMessage = message; lastExtras = extras
        }

        override fun i(message: String, extras: Map<String, Any?>) {
            lastMessage = message; lastExtras = extras
        }

        override fun w(message: String, throwable: Throwable?, extras: Map<String, Any?>) {
            lastMessage = message; lastThrowable = throwable; lastExtras = extras
        }

        override fun e(message: String, throwable: Throwable?, extras: Map<String, Any?>) {
            lastMessage = message; lastThrowable = throwable; lastExtras = extras
        }
    }
}
