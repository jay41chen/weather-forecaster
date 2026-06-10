package com.weather.core.model

data class CurrentWeather(
    val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
    val pressure: Int,
    val timestamp: Long
)
