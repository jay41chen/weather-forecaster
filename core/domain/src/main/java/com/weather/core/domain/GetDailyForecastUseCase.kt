package com.weather.core.domain

import com.weather.core.model.DailyForecast
import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDailyForecastUseCase @Inject constructor(
    private val repo: WeatherRepository,
    private val syncWeather: SyncWeatherUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(cityName: String): Flow<Resource<List<DailyForecast>>> = flow {
        val cached = repo.observeDailyForecasts(cityName).first()
        if (cached.isNotEmpty()) emit(Resource.Success(cached)) else emit(Resource.Loading)

        val syncResult = syncWeather(cityName)
        if (syncResult is Resource.Error && cached.isEmpty()) {
            emit(syncResult)
        }
    }.flatMapLatest { state ->
        repo.observeDailyForecasts(cityName).map { forecasts ->
            if (forecasts.isNotEmpty()) Resource.Success(forecasts) else state
        }
    }
}
