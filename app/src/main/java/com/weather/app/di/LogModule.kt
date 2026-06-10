package com.weather.app.di

import com.weather.core.logging.LogPortFactory
import com.weather.core.logging.TimberLogPortFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LogModule {
    @Binds
    @Singleton
    abstract fun bindLogPortFactory(impl: TimberLogPortFactory): LogPortFactory
}
