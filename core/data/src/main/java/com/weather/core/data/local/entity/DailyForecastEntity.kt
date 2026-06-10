package com.weather.core.data.local.entity

import androidx.room.Entity

@Entity(tableName = "daily_forecasts", primaryKeys = ["cityName", "date"])
data class DailyForecastEntity(
    val cityName: String,
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double
)
