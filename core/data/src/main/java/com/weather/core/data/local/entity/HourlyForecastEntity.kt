package com.weather.core.data.local.entity

import androidx.room.Entity

@Entity(tableName = "hourly_forecasts", primaryKeys = ["cityName", "timestamp"])
data class HourlyForecastEntity(
    val cityName: String,
    val timestamp: Long,
    val temperature: Double,
    val iconCode: String,
    val description: String
)
