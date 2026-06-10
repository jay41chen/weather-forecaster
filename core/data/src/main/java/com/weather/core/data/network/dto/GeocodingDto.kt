package com.weather.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingDto(
    @SerialName("name") val name: String,
    @SerialName("country") val country: String,
    @SerialName("state") val state: String? = null,
    @SerialName("lat") val lat: Double,
    @SerialName("lon") val lon: Double
)
