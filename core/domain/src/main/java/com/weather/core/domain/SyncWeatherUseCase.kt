package com.weather.core.domain

import com.weather.core.logging.LogPortFactory
import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWeatherUseCase @Inject constructor(
    private val repo: WeatherRepository,
    logFactory: LogPortFactory
) {
    private val log = logFactory.create("SyncWeather")
    private val cityMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastSynced = ConcurrentHashMap<String, Long>()

    suspend operator fun invoke(cityName: String): Resource<Unit> {
        val mutex = cityMutexes.computeIfAbsent(cityName) { Mutex() }
        return mutex.withLock {
            val last = lastSynced[cityName] ?: 0L
            if (System.currentTimeMillis() - last < CACHE_TTL_MS) {
                log.d("Cache hit", mapOf("city" to cityName))
                return@withLock Resource.Success(Unit)
            }
            val result = repo.sync(cityName)
            if (result is Resource.Success) {
                lastSynced[cityName] = System.currentTimeMillis()
            }
            result
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 30 * 60_000L
    }
}
