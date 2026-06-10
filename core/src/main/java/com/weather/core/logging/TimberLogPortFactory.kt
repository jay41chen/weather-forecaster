package com.weather.core.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimberLogPortFactory @Inject constructor() : LogPortFactory {
    override fun create(tag: String): LogPort = TimberLogAdapter(tag)
}
