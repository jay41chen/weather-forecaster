package com.weather.core.logging

interface LogPortFactory {
    fun create(tag: String): LogPort
}
