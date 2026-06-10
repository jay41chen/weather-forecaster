package com.weather.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeatherDto(
    @SerialName("name") val name: String,
    @SerialName("main") val main: MainDto,
    @SerialName("weather") val weather: List<WeatherDescDto>,
    @SerialName("wind") val wind: WindDto,
    @SerialName("dt") val dt: Long,
    @SerialName("sys") val sys: SysDto
)

@Serializable
data class MainDto(
    @SerialName("temp") val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("temp_min") val tempMin: Double,
    @SerialName("temp_max") val tempMax: Double,
    @SerialName("humidity") val humidity: Int,
    @SerialName("pressure") val pressure: Int
)

@Serializable
data class WeatherDescDto(
    @SerialName("description") val description: String,
    @SerialName("icon") val icon: String
)

@Serializable
data class WindDto(
    @SerialName("speed") val speed: Double
)

@Serializable
data class SysDto(
    @SerialName("country") val country: String = ""
)
