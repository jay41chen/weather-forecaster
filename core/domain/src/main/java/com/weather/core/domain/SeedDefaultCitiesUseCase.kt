package com.weather.core.domain

import com.weather.core.model.City
import com.weather.core.repository.CityRepository
import javax.inject.Inject

class SeedDefaultCitiesUseCase @Inject constructor(
    private val repo: CityRepository
) {
    suspend operator fun invoke() {
        if (repo.count() > 0) return
        DEFAULT_CITIES.forEach { repo.saveCity(it) }
        repo.selectCity("London")
    }

    companion object {
        private val DEFAULT_CITIES = listOf(
            City("Taipei",    "TW", null,               25.0330,  121.5654),
            City("London",    "GB", null,              51.5074,   -0.1278),
            City("New York",  "US", "New York",        40.7128,  -74.0060),
            City("Tokyo",     "JP", null,              35.6762,  139.6503),
            City("Paris",     "FR", null,              48.8566,    2.3522),
            City("Sydney",    "AU", "New South Wales", -33.8688, 151.2093),
            City("Berlin",    "DE", null,              52.5200,   13.4050),
            City("Toronto",   "CA", "Ontario",         43.6532,  -79.3832),
            City("Singapore", "SG", null,               1.3521,  103.8198),
            City("Rome",      "IT", null,              41.9028,   12.4964),
            City("São Paulo", "BR", "São Paulo",       -23.5505, -46.6333),
        )
    }
}
