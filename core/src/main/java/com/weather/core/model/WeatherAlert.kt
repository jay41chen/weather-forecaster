package com.weather.core.model

data class WeatherAlert(
    val cityName: String,
    val type: String,
    val message: String,
    val timestamp: Long
)
