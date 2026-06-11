package com.weather.core.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class CompositeLogPortTest {

    @Test
    fun `d fans out to all delegates`() {
        val recorder1 = RecordingLogPort()
        val recorder2 = RecordingLogPort()
        val composite = CompositeLogPort(listOf(recorder1, recorder2))

        composite.d("hello", mapOf("key" to "value"))

        assertEquals(1, recorder1.calls.size)
        assertEquals(1, recorder2.calls.size)
        recorder1.calls[0].let {
            assertEquals("d", it.level)
            assertEquals("hello", it.message)
            assertEquals(mapOf("key" to "value"), it.extras)
        }
        recorder2.calls[0].let {
            assertEquals("d", it.level)
            assertEquals("hello", it.message)
        }
    }

    @Test
    fun `w fans out with throwable`() {
        val recorder = RecordingLogPort()
        val composite = CompositeLogPort(listOf(recorder))
        val error = RuntimeException("boom")

        composite.w("warning", error, mapOf("ctx" to 42))

        recorder.calls[0].let {
            assertEquals("w", it.level)
            assertEquals("warning", it.message)
            assertEquals(error, it.throwable)
            assertEquals(mapOf("ctx" to 42), it.extras)
        }
    }

    @Test
    fun `e fans out with throwable`() {
        val recorder = RecordingLogPort()
        val composite = CompositeLogPort(listOf(recorder))
        val error = RuntimeException("fatal")

        composite.e("error", error)

        recorder.calls[0].let {
            assertEquals("e", it.level)
            assertEquals("error", it.message)
            assertEquals(error, it.throwable)
        }
    }

    @Test
    fun `i fans out to all delegates`() {
        val recorder1 = RecordingLogPort()
        val recorder2 = RecordingLogPort()
        val recorder3 = RecordingLogPort()
        val composite = CompositeLogPort(listOf(recorder1, recorder2, recorder3))

        composite.i("info")

        assertEquals(1, recorder1.calls.size)
        assertEquals(1, recorder2.calls.size)
        assertEquals(1, recorder3.calls.size)
    }

    @Test
    fun `empty delegates list does not crash`() {
        val composite = CompositeLogPort(emptyList())

        composite.d("no one listening")
        composite.i("still fine")
        composite.w("also fine", RuntimeException())
        composite.e("and fine", RuntimeException())
    }

    @Test
    fun `factory creates composite from multiple factories`() {
        val recorder1 = RecordingLogPort()
        val recorder2 = RecordingLogPort()
        val factory = CompositeLogPortFactory(
            listOf(
                object : LogPortFactory {
                    override fun create(tag: String) = recorder1
                },
                object : LogPortFactory {
                    override fun create(tag: String) = recorder2
                }
            )
        )

        val log = factory.create("TestTag")
        log.d("test")

        assertEquals(1, recorder1.calls.size)
        assertEquals(1, recorder2.calls.size)
    }

    // --- helpers ---

    private data class LogCall(
        val level: String,
        val message: String,
        val throwable: Throwable? = null,
        val extras: Map<String, Any?> = emptyMap()
    )

    private class RecordingLogPort : LogPort {
        val calls = mutableListOf<LogCall>()

        override fun d(message: String, extras: Map<String, Any?>) {
            calls.add(LogCall("d", message, extras = extras))
        }

        override fun i(message: String, extras: Map<String, Any?>) {
            calls.add(LogCall("i", message, extras = extras))
        }

        override fun w(message: String, throwable: Throwable?, extras: Map<String, Any?>) {
            calls.add(LogCall("w", message, throwable, extras))
        }

        override fun e(message: String, throwable: Throwable?, extras: Map<String, Any?>) {
            calls.add(LogCall("e", message, throwable, extras))
        }
    }
}
