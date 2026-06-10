package com.weather.core.domain

import com.weather.core.model.City
import com.weather.core.repository.CityRepository
import javax.inject.Inject

class RemoveCityUseCase @Inject constructor(
    private val repo: CityRepository
) {
    suspend operator fun invoke(city: City) = repo.removeCity(city)
}
