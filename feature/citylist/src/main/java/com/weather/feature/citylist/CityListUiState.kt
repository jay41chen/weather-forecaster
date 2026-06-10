package com.weather.feature.citylist

import com.weather.core.model.City

data class CityListUiState(
    val savedCities: List<City> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<City> = emptyList(),
    val isSearching: Boolean = false,
    val selectedCityName: String? = null
)
