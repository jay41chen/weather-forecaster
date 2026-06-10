package com.weather.core.repository

import com.weather.core.model.City
import com.weather.core.model.Resource
import kotlinx.coroutines.flow.Flow

interface CityRepository {
    fun observeSavedCities(): Flow<List<City>>
    fun observeSelectedCityName(): Flow<String?>
    suspend fun searchCities(query: String): Resource<List<City>>
    suspend fun saveCity(city: City)
    suspend fun removeCity(city: City)
    suspend fun selectCity(cityName: String)
    suspend fun initializeIfNeeded()
}
