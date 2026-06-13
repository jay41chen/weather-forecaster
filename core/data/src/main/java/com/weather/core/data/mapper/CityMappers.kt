package com.weather.core.data.mapper

import com.weather.core.data.local.entity.CityEntity
import com.weather.core.model.City

fun CityEntity.toDomain(): City = City(
    name = name,
    country = country,
    state = state,
    latitude = latitude,
    longitude = longitude
)

fun City.toEntity(): CityEntity = CityEntity(
    id = "$name,$country,${state.orEmpty()}",
    name = name,
    country = country,
    state = state,
    latitude = latitude,
    longitude = longitude
)
