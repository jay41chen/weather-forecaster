package com.weather.core.domain

import com.weather.core.model.CurrentWeather
import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCurrentWeatherUseCase @Inject constructor(
    private val repo: WeatherRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(cityName: String): Flow<Resource<CurrentWeather>> = flow {
        val cached = repo.observeCurrentWeather(cityName).first()
        if (cached != null) emit(Resource.Success(cached)) else emit(Resource.Loading)

        val syncResult = repo.sync(cityName)
        if (syncResult is Resource.Error && cached == null) {
            emit(syncResult)
        }
    }.flatMapLatest { state ->
        repo.observeCurrentWeather(cityName).map { weather ->
            if (weather != null) Resource.Success(weather) else state
        }
    }
}
