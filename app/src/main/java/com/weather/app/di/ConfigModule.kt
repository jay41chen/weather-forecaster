package com.weather.app.di

import com.weather.core.config.FeatureTogglePort
import com.weather.core.config.RemoteFeatureToggleAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigModule {
    @Binds
    @Singleton
    abstract fun bindFeatureToggle(impl: RemoteFeatureToggleAdapter): FeatureTogglePort
}
