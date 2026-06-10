package com.weather.core.data.network

import com.weather.core.data.network.dto.CurrentWeatherDto
import com.weather.core.data.network.dto.ForecastDto
import com.weather.core.data.network.dto.GeocodingDto
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitClient {

    @GET("/data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityName: String,
        @Query("units") units: String = "metric"
    ): CurrentWeatherDto

    @GET("/data/2.5/forecast")
    suspend fun getForecast(
        @Query("q") cityName: String,
        @Query("units") units: String = "metric"
    ): ForecastDto

    @GET("/geo/1.0/direct")
    suspend fun searchCity(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): List<GeocodingDto>

    @GET("/geo/1.0/reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("limit") limit: Int = 1
    ): List<GeocodingDto>

    @GET("/data/2.5/weather")
    suspend fun getCurrentWeatherByCoords(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric"
    ): CurrentWeatherDto
}
