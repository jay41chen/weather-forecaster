package com.weather.feature.citylist

sealed interface CityListEvent {
    data object NavigateBack : CityListEvent
}
