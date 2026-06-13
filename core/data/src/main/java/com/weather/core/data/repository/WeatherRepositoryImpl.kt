package com.weather.core.data.repository

import com.weather.core.data.local.dao.WeatherDao
import com.weather.core.data.mapper.toDomain
import com.weather.core.data.mapper.toEntity
import com.weather.core.model.CurrentWeather
import com.weather.core.model.DailyForecast
import com.weather.core.model.HourlyForecast
import com.weather.core.model.Resource
import com.weather.core.logging.LogPortFactory
import com.weather.core.network.WeatherApiService
import com.weather.core.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApiService,
    private val weatherDao: WeatherDao,
    private val logFactory: LogPortFactory
) : WeatherRepository {

    private val log = logFactory.create("WeatherRepo")

    override fun observeCurrentWeather(cityName: String): Flow<CurrentWeather?> {
        return weatherDao.observeCurrentWeather(cityName).map { it?.toDomain() }
    }

    override fun observeDailyForecasts(cityName: String): Flow<List<DailyForecast>> {
        return weatherDao.observeDailyForecasts(cityName).map { list -> list.map { it.toDomain() } }
    }

    override fun observeHourlyForecasts(cityName: String): Flow<List<HourlyForecast>> {
        return weatherDao.observeHourlyForecasts(cityName).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun sync(cityName: String): Resource<Unit> {
        return try {
            log.d("Syncing", mapOf("city" to cityName))
            val current = api.getCurrentWeather(cityName)
            weatherDao.insertCurrentWeather(current.toEntity())

            val forecast = api.getForecast(cityName)
            weatherDao.replaceDailyForecasts(cityName, forecast.daily.map { it.toEntity(cityName) })
            weatherDao.replaceHourlyForecasts(cityName, forecast.hourly.map { it.toEntity(cityName) })

            log.i("Sync success", mapOf("city" to cityName, "source" to "network"))
            Resource.Success(Unit)
        } catch (e: Exception) {
            log.e("Sync failed", e, mapOf("city" to cityName, "error_type" to e.toApiError()::class.simpleName))
            Resource.Error(e.message ?: "Sync failed", e.toApiError(), e)
        }
    }

    override suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double): CurrentWeather? {
        return try {
            val current = api.getCurrentWeatherByCoords(lat, lon)
            weatherDao.insertCurrentWeather(current.toEntity())
            current
        } catch (e: Exception) {
            null
        }
    }
}
