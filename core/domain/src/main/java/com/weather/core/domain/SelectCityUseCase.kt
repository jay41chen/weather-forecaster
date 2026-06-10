package com.weather.core.domain

import com.weather.core.repository.CityRepository
import javax.inject.Inject

class SelectCityUseCase @Inject constructor(
    private val repo: CityRepository
) {
    suspend operator fun invoke(cityName: String) = repo.selectCity(cityName)
}
