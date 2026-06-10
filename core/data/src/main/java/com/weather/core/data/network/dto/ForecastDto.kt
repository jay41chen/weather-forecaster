package com.weather.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastDto(
    @SerialName("list") val list: List<ForecastItemDto>,
    @SerialName("city") val city: ForecastCityDto
)

@Serializable
data class ForecastItemDto(
    @SerialName("dt") val dt: Long,
    @SerialName("main") val main: MainDto,
    @SerialName("weather") val weather: List<WeatherDescDto>,
    @SerialName("wind") val wind: WindDto,
    @SerialName("dt_txt") val dtTxt: String
)

@Serializable
data class ForecastCityDto(
    @SerialName("name") val name: String,
    @SerialName("country") val country: String
)
