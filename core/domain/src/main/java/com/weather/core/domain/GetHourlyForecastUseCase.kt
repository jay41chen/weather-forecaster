package com.weather.core.domain

import com.weather.core.model.HourlyForecast
import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHourlyForecastUseCase @Inject constructor(
    private val repo: WeatherRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(cityName: String): Flow<Resource<List<HourlyForecast>>> = flow {
        val cached = repo.observeHourlyForecasts(cityName).first()
        if (cached.isNotEmpty()) emit(Resource.Success(cached)) else emit(Resource.Loading)

        val syncResult = repo.sync(cityName)
        if (syncResult is Resource.Error && cached.isEmpty()) {
            emit(syncResult)
        }
    }.flatMapLatest { state ->
        repo.observeHourlyForecasts(cityName).map { forecasts ->
            if (forecasts.isNotEmpty()) Resource.Success(forecasts) else state
        }
    }
}
