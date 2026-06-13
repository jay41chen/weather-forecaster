package com.weather.core.domain

import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRepository
import javax.inject.Inject

class RefreshWeatherUseCase @Inject constructor(
    private val repo: WeatherRepository
) {
    suspend operator fun invoke(cityName: String): Resource<Unit> = repo.sync(cityName)
}
