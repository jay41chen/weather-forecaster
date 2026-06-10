package com.weather.feature.citylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.domain.GetSavedCitiesUseCase
import com.weather.core.domain.GetSelectedCityUseCase
import com.weather.core.domain.RemoveCityUseCase
import com.weather.core.domain.SaveCityUseCase
import com.weather.core.domain.SearchCitiesUseCase
import com.weather.core.domain.SelectCityUseCase
import com.weather.core.model.City
import com.weather.core.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class CityListViewModel @Inject constructor(
    private val getSavedCities: GetSavedCitiesUseCase,
    private val getSelectedCity: GetSelectedCityUseCase,
    private val searchCities: SearchCitiesUseCase,
    private val saveCity: SaveCityUseCase,
    private val removeCity: RemoveCityUseCase,
    private val selectCity: SelectCityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CityListUiState())
    val uiState: StateFlow<CityListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            getSavedCities().collect { cities ->
                _uiState.update { it.copy(savedCities = cities) }
            }
        }
        viewModelScope.launch {
            getSelectedCity().collect { cityName ->
                _uiState.update { it.copy(selectedCityName = cityName) }
            }
        }
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .flatMapLatest { query ->
                    flow {
                        if (query.isBlank()) {
                            emit(emptyList<City>())
                            return@flow
                        }
                        _uiState.update { it.copy(isSearching = true) }
                        val result = searchCities(query)
                        emit(if (result is Resource.Success) result.data else emptyList())
                    }
                }
                .collectLatest { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun onCitySelect(city: City, onNavigateBack: () -> Unit) {
        viewModelScope.launch {
            selectCity(city.name)
            onNavigateBack()
        }
    }

    fun onCityAdd(city: City) {
        viewModelScope.launch { saveCity(city) }
    }

    fun onCityRemove(city: City) {
        viewModelScope.launch { removeCity(city) }
    }
}
