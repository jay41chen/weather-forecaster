package com.weather.core.domain

import com.weather.core.logging.LogPortFactory
import com.weather.core.repository.CityRepository
import com.weather.core.repository.LocationRepository
import com.weather.core.repository.WeatherRepository
import javax.inject.Inject

class DetectAndSelectCityUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val cityRepository: CityRepository,
    logFactory: LogPortFactory
) {
    private val log = logFactory.create("DetectCity")
    suspend operator fun invoke() {
        try {
            val coordinates = locationRepository.getCurrentLocation() ?: return
            val weather = weatherRepository.getCurrentWeatherByCoords(
                coordinates.latitude,
                coordinates.longitude
            ) ?: return
            cityRepository.selectCity(weather.cityName)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e("Failed to detect city from location", e)
        }
    }
}
