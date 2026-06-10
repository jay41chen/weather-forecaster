package com.weather.core.network

import com.weather.core.model.City
import com.weather.core.model.CurrentWeather
import com.weather.core.model.DailyForecast
import com.weather.core.model.HourlyForecast

interface WeatherApiService {
    suspend fun getCurrentWeather(cityName: String): CurrentWeather
    suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double): CurrentWeather
    suspend fun getForecast(cityName: String): ForecastResult
    suspend fun searchCities(query: String): List<City>
}

data class ForecastResult(
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>
)
