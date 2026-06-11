package com.weather.core.repository

import com.weather.core.model.CurrentWeather
import com.weather.core.model.WeatherAlert
import kotlinx.coroutines.flow.Flow

interface WeatherRealtimeService {
    fun observeWeatherUpdates(): Flow<CurrentWeather>
    fun observeWeatherAlerts(): Flow<WeatherAlert>
    suspend fun connect()
    suspend fun disconnect()
    suspend fun subscribeCities(cityNames: List<String>)
}
