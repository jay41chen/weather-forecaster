package com.weather.core.repository

import com.weather.core.model.CurrentWeather
import com.weather.core.model.DailyForecast
import com.weather.core.model.HourlyForecast
import com.weather.core.model.Resource
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun observeCurrentWeather(cityName: String): Flow<CurrentWeather?>
    fun observeDailyForecasts(cityName: String): Flow<List<DailyForecast>>
    fun observeHourlyForecasts(cityName: String): Flow<List<HourlyForecast>>
    suspend fun sync(cityName: String): Resource<Unit>
    suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double): CurrentWeather?
}
