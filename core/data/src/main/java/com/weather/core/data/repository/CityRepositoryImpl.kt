package com.weather.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.weather.core.data.local.dao.CityDao
import com.weather.core.data.mapper.toDomain
import com.weather.core.data.mapper.toEntity
import com.weather.core.model.City
import com.weather.core.logging.LogPortFactory
import com.weather.core.model.Resource
import com.weather.core.network.WeatherApiService
import com.weather.core.repository.CityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CityRepositoryImpl @Inject constructor(
    private val cityDao: CityDao,
    private val api: WeatherApiService,
    private val dataStore: DataStore<Preferences>,
    private val logFactory: LogPortFactory
) : CityRepository {

    private val log = logFactory.create("CityRepo")

    private val selectedCityKey = stringPreferencesKey("selected_city")

    override fun observeSavedCities(): Flow<List<City>> {
        return cityDao.observeAllCities().map { list -> list.map { it.toDomain() } }
    }

    override fun observeSelectedCityName(): Flow<String?> {
        return dataStore.data.map { prefs -> prefs[selectedCityKey] }
    }

    override suspend fun searchCities(query: String): Resource<List<City>> {
        log.d("Searching cities", mapOf("query" to query))
        return try {
            val cities = api.searchCities(query)
            log.i("Search success", mapOf("query" to query, "source" to "network", "count" to cities.size))
            Resource.Success(cities)
        } catch (e: Exception) {
            log.e("Search failed", e, mapOf("query" to query, "error_type" to e.toApiError()::class.simpleName))
            Resource.Error(e.message ?: "Search failed", e.toApiError(), e)
        }
    }

    override suspend fun saveCity(city: City) {
        log.i("Saving city", mapOf("city" to city.name))
        cityDao.insertCity(city.toEntity())
    }

    override suspend fun removeCity(city: City) {
        log.i("Removing city", mapOf("city" to city.name))
        cityDao.deleteCity(city.toEntity())
    }

    override suspend fun selectCity(cityName: String) {
        log.i("Selecting city", mapOf("city" to cityName))
        dataStore.edit { prefs -> prefs[selectedCityKey] = cityName }
    }

    override suspend fun count(): Int = cityDao.count()
}
