package com.weather.core.data.di

import com.weather.core.data.network.RetrofitWeatherApiService
import com.weather.core.data.repository.CityRepositoryImpl
import com.weather.core.data.repository.WeatherRepositoryImpl
import com.weather.core.network.WeatherApiService
import com.weather.core.repository.CityRepository
import com.weather.core.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindWeatherApi(impl: RetrofitWeatherApiService): WeatherApiService

    @Binds
    @Singleton
    abstract fun bindWeatherRepo(impl: WeatherRepositoryImpl): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindCityRepo(impl: CityRepositoryImpl): CityRepository
}
