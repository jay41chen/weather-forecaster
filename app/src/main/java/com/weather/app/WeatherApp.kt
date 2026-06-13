package com.weather.app

import android.app.Application
import com.weather.core.domain.SeedDefaultCitiesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WeatherApp : Application() {

    @Inject
    lateinit var seedDefaultCities: SeedDefaultCitiesUseCase

    // Runs before any Activity/ViewModel exists; no presentation-layer scope available yet.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        appScope.launch {
            seedDefaultCities()
        }
    }
}
