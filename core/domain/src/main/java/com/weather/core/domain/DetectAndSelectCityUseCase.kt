package com.weather.core.domain

import com.weather.core.repository.CityRepository
import com.weather.core.repository.LocationRepository
import com.weather.core.repository.WeatherRepository
import javax.inject.Inject

class DetectAndSelectCityUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val cityRepository: CityRepository
) {
    suspend operator fun invoke() {
        val coordinates = locationRepository.getCurrentLocation() ?: return
        val weather = weatherRepository.getCurrentWeatherByCoords(
            coordinates.latitude,
            coordinates.longitude
        ) ?: return
        cityRepository.selectCity(weather.cityName)
    }
}
