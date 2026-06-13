package com.weather.feature.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.config.FeatureFlag
import com.weather.core.config.FeatureTogglePort
import com.weather.core.config.isEnabled
import com.weather.core.domain.GetCurrentWeatherUseCase
import com.weather.core.domain.GetDailyForecastUseCase
import com.weather.core.domain.GetHourlyForecastUseCase
import com.weather.core.domain.GetSelectedCityUseCase
import com.weather.core.domain.RefreshWeatherUseCase
import com.weather.core.di.ApplicationScope
import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRealtimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getCurrentWeather: GetCurrentWeatherUseCase,
    private val getDailyForecast: GetDailyForecastUseCase,
    private val getHourlyForecast: GetHourlyForecastUseCase,
    private val refreshWeather: RefreshWeatherUseCase,
    private val getSelectedCity: GetSelectedCityUseCase,
    private val featureToggle: FeatureTogglePort,
    private val realtime: WeatherRealtimeService,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    val showHourlyForecast: Boolean
        get() = featureToggle.isEnabled(FeatureFlag.HOURLY_FORECAST_ENABLED)

    init {
        viewModelScope.launch {
            if (featureToggle.isEnabled(FeatureFlag.SOCKET_IO_ENABLED)) {
                realtime.connect()
            }

            var previousCity: String? = null
            getSelectedCity().filterNotNull().collectLatest { cityName ->
                _uiState.update { it.copy(cityName = cityName, isLoading = true, error = null) }
                if (featureToggle.isEnabled(FeatureFlag.SOCKET_IO_ENABLED)) {
                    previousCity?.let { realtime.unsubscribeCities(listOf(it)) }
                    realtime.subscribeCities(listOf(cityName))
                    previousCity = cityName
                }
                loadWeather(cityName)
            }
        }

        if (featureToggle.isEnabled(FeatureFlag.WEATHER_ALERTS_ENABLED)) {
            viewModelScope.launch {
                realtime.observeWeatherAlerts().collect { alert ->
                    _uiState.update { it.copy(alertMessage = alert.message) }
                }
            }
        }
    }

    private suspend fun loadWeather(cityName: String) = supervisorScope {
        launch {
            getCurrentWeather(cityName).collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update {
                        it.copy(currentWeather = result.data, isLoading = false, error = null)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(error = result.apiError, isLoading = false)
                    }
                    Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
        launch {
            getDailyForecast(cityName).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(dailyForecasts = result.data) }
                }
            }
        }
        launch {
            getHourlyForecast(cityName).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(hourlyForecasts = result.data) }
                }
            }
        }
    }

    fun refresh() {
        val city = _uiState.value.cityName
        if (city.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = refreshWeather(city)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    error = if (result is Resource.Error) result.apiError else null
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        if (featureToggle.isEnabled(FeatureFlag.SOCKET_IO_ENABLED)) {
            appScope.launch { realtime.disconnect() }
        }
    }
}
