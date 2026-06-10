package com.weather.core.domain

import com.weather.core.model.City
import com.weather.core.repository.CityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavedCitiesUseCase @Inject constructor(
    private val repo: CityRepository
) {
    operator fun invoke(): Flow<List<City>> = repo.observeSavedCities()
}
