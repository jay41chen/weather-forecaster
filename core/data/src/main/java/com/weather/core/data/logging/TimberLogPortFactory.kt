package com.weather.core.data.logging

import com.weather.core.logging.LogPort
import com.weather.core.logging.LogPortFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimberLogPortFactory @Inject constructor() : LogPortFactory {
    override fun create(tag: String): LogPort = TimberLogAdapter(tag)
}
