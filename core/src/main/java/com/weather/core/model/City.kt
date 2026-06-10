package com.weather.core.model

data class City(
    val name: String,
    val country: String,
    val state: String?,
    val latitude: Double,
    val longitude: Double
)
