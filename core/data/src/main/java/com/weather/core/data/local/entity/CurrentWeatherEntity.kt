package com.weather.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
    val pressure: Int,
    val timestamp: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
