package com.weather.core.model

import java.time.LocalDate

data class DailyForecast(
    val date: LocalDate,
    val maxTemp: Double,
    val minTemp: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double
)
