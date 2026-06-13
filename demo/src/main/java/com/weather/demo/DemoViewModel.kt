package com.weather.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.config.FeatureFlag
import com.weather.core.config.FeatureToggleMockAdapter
import com.weather.core.model.Resource
import com.weather.core.repository.CityRepository
import com.weather.core.repository.LocationRepository
import com.weather.core.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DemoViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val cityRepository: CityRepository,
    private val weatherRepository: WeatherRepository,
    private val featureToggle: FeatureToggleMockAdapter
) : ViewModel() {

    private val _locationResult = MutableStateFlow("—")
    val locationResult: StateFlow<String> = _locationResult.asStateFlow()

    private val _searchResult = MutableStateFlow("—")
    val searchResult: StateFlow<String> = _searchResult.asStateFlow()

    private val _weatherResult = MutableStateFlow("—")
    val weatherResult: StateFlow<String> = _weatherResult.asStateFlow()

    val featureFlags: StateFlow<Map<String, Boolean>> = featureToggle.configs
        .map { map -> map.filterValues { it is Boolean }.mapValues { it.value as Boolean } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, featureToggle.configs.value.filterValues { it is Boolean }.mapValues { it.value as Boolean })

    fun toggleFlag(flag: FeatureFlag, enabled: Boolean) {
        featureToggle.setFlag(flag, enabled)
    }

    fun testLocation() {
        viewModelScope.launch {
            _locationResult.value = "…"
            val coordinates = locationRepository.getCurrentLocation()
            _locationResult.value = if (coordinates != null)
                "lat=${coordinates.latitude}, lon=${coordinates.longitude}"
            else
                "null — no GPS fix or permission denied"
        }
    }

    fun searchCities(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _searchResult.value = "…"
            _searchResult.value = when (val r = cityRepository.searchCities(query)) {
                is Resource.Success -> r.data
                    .joinToString("\n") { "${it.name}, ${it.country}" }
                    .ifEmpty { "(empty)" }
                is Resource.Error -> "Error: ${r.message}"
                Resource.Loading -> "…"
            }
        }
    }

    fun syncWeather(cityName: String) {
        if (cityName.isBlank()) return
        viewModelScope.launch {
            _weatherResult.value = "…"
            _weatherResult.value = when (val r = weatherRepository.forceSync(cityName)) {
                is Resource.Success -> "OK"
                is Resource.Error -> "Error: ${r.message}"
                Resource.Loading -> "…"
            }
        }
    }
}
