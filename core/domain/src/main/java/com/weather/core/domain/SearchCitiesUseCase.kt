package com.weather.core.domain

import com.weather.core.model.City
import com.weather.core.model.Resource
import com.weather.core.repository.CityRepository
import javax.inject.Inject

class SearchCitiesUseCase @Inject constructor(
    private val repo: CityRepository
) {
    suspend operator fun invoke(query: String): Resource<List<City>> = repo.searchCities(query)
}
