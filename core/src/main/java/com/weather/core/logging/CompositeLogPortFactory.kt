package com.weather.core.logging

class CompositeLogPortFactory(
    private val factories: List<LogPortFactory>
) : LogPortFactory {
    override fun create(tag: String): LogPort =
        CompositeLogPort(factories.map { it.create(tag) })
}
