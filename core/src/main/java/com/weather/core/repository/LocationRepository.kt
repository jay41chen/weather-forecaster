package com.weather.core.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
}
