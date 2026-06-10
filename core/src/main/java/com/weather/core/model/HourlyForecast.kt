package com.weather.core.model

data class HourlyForecast(
    val timestamp: Long,
    val temperature: Double,
    val iconCode: String,
    val description: String
)
