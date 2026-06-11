package com.weather.demo.di

import com.weather.core.config.FeatureFlag
import com.weather.core.config.FeatureToggleMockAdapter
import com.weather.core.config.FeatureTogglePort
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DemoConfigModule {

    @Provides
    @Singleton
    fun provideMockAdapter(): FeatureToggleMockAdapter = FeatureToggleMockAdapter(
        initial = mapOf(
            FeatureFlag.SOCKET_IO_ENABLED.key to false,
            FeatureFlag.CITY_SEARCH_ENABLED.key to true,
            FeatureFlag.HOURLY_FORECAST_ENABLED.key to true,
            FeatureFlag.OFFLINE_BANNER_ENABLED.key to true,
            FeatureFlag.WEATHER_ALERTS_ENABLED.key to false,
        )
    )

    @Provides
    @Singleton
    fun provideFeatureToggle(impl: FeatureToggleMockAdapter): FeatureTogglePort = impl
}
