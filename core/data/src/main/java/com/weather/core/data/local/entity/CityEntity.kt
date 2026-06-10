package com.weather.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cities")
data class CityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val state: String?,
    val latitude: Double,
    val longitude: Double
)
