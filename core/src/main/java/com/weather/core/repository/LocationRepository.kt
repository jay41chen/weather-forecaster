package com.weather.core.repository

import com.weather.core.model.Coordinates

interface LocationRepository {
    suspend fun getCurrentLocation(): Coordinates?
}
