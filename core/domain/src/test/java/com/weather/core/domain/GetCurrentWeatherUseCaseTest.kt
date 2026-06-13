package com.weather.core.domain

import com.weather.core.model.CurrentWeather
import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetCurrentWeatherUseCaseTest {

    private val repo: WeatherRepository = mockk()
    private val syncWeather: SyncWeatherUseCase = mockk()
    private lateinit var useCase: GetCurrentWeatherUseCase

    private val cityName = "Taipei"

    private val cachedWeather = CurrentWeather(
        cityName = cityName,
        country = "TW",
        temperature = 25.0,
        feelsLike = 27.0,
        description = "Cloudy",
        iconCode = "03d",
        humidity = 70,
        windSpeed = 2.0,
        pressure = 1010,
        timestamp = 1000L
    )

    private val updatedWeather = CurrentWeather(
        cityName = cityName,
        country = "TW",
        temperature = 28.0,
        feelsLike = 30.0,
        description = "Clear",
        iconCode = "01d",
        humidity = 60,
        windSpeed = 3.5,
        pressure = 1013,
        timestamp = 2000L
    )

    @Before
    fun setUp() {
        useCase = GetCurrentWeatherUseCase(repo, syncWeather)
    }

    /**
     * Cache + network OK:
     * 1. observeCurrentWeather.first() returns cached -> outer emits Success(cached)
     * 2. flatMapLatest subscribes inner flow -> emits Success(cached)
     * 3. sync() succeeds and updates Room -> inner flow emits Success(updated)
     * Collector sees: [Success(cached), Success(updated)]
     */
    @Test
    fun `cache hit then sync success emits cached first then updated data`() = runTest {
        val weatherFlow = MutableStateFlow<CurrentWeather?>(cachedWeather)

        every { repo.observeCurrentWeather(cityName) } returns weatherFlow
        coEvery { syncWeather(cityName) } coAnswers {
            weatherFlow.value = updatedWeather
            Resource.Success(Unit)
        }

        val emissions = useCase(cityName).take(2).toList()

        // First: cached data from Room
        assertTrue(emissions[0] is Resource.Success)
        assertEquals(cachedWeather, (emissions[0] as Resource.Success).data)

        // Second: updated data after sync wrote to Room
        assertTrue(emissions[1] is Resource.Success)
        assertEquals(updatedWeather, (emissions[1] as Resource.Success).data)
    }

    /**
     * Cache + network fail:
     * 1. observeCurrentWeather.first() returns cached -> outer emits Success(cached)
     * 2. flatMapLatest subscribes inner flow -> emits Success(cached)
     * 3. sync() fails, but cached != null -> outer does NOT emit Error
     * 4. Outer flow completes; inner flow stays alive on the same cached value
     * Collector sees: [Success(cached)] and the flow remains open (no error surfaces).
     */
    @Test
    fun `cache hit then sync failure keeps showing cached data without error`() = runTest {
        val weatherFlow = MutableStateFlow<CurrentWeather?>(cachedWeather)

        every { repo.observeCurrentWeather(cityName) } returns weatherFlow
        coEvery { syncWeather(cityName) } returns Resource.Error("Network error", throwable = IOException("No internet"))

        // take(1) because only one distinct emission: Success(cached).
        // The flow stays alive afterwards (no error is ever emitted).
        val emissions = useCase(cityName).take(1).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is Resource.Success)
        assertEquals(cachedWeather, (emissions[0] as Resource.Success).data)
    }

    /**
     * No cache + network fail:
     * 1. observeCurrentWeather.first() returns null -> outer emits Loading
     * 2. flatMapLatest subscribes inner (weather=null) -> emits Loading (the state)
     * 3. sync() fails, cached == null -> outer emits Error
     * 4. flatMapLatest cancels previous inner, subscribes new inner with state=Error
     *    weather is still null -> emits Error
     * Collector sees: [Loading, Error]
     */
    @Test
    fun `no cache and sync failure emits Loading then Error`() = runTest {
        val weatherFlow = MutableStateFlow<CurrentWeather?>(null)

        every { repo.observeCurrentWeather(cityName) } returns weatherFlow
        coEvery { syncWeather(cityName) } returns Resource.Error("Sync failed", throwable = IOException("No internet"))

        val emissions = useCase(cityName).take(2).toList()

        // First: Loading because cache was empty
        assertTrue("Expected Loading but got ${emissions[0]}", emissions[0] is Resource.Loading)

        // Second: Error because sync failed and no cached data to fall back on
        assertTrue("Expected Error but got ${emissions[1]}", emissions[1] is Resource.Error)
        assertEquals("Sync failed", (emissions[1] as Resource.Error).message)
    }

    /**
     * No cache + network OK:
     * 1. observeCurrentWeather.first() returns null -> outer emits Loading
     * 2. flatMapLatest subscribes inner (weather=null) -> emits Loading
     * 3. sync() succeeds, updates weatherFlow -> inner emits Success(updated)
     * 4. Outer flow completes (sync succeeded, no Error to emit)
     * Collector sees: [Loading, Success(updated)]
     */
    @Test
    fun `no cache and sync success emits Loading then data`() = runTest {
        val weatherFlow = MutableStateFlow<CurrentWeather?>(null)

        every { repo.observeCurrentWeather(cityName) } returns weatherFlow
        coEvery { syncWeather(cityName) } coAnswers {
            weatherFlow.value = updatedWeather
            Resource.Success(Unit)
        }

        val emissions = useCase(cityName).take(2).toList()

        // First: Loading because cache was empty
        assertTrue("Expected Loading but got ${emissions[0]}", emissions[0] is Resource.Loading)

        // Second: data from Room after sync
        assertTrue("Expected Success but got ${emissions[1]}", emissions[1] is Resource.Success)
        assertEquals(updatedWeather, (emissions[1] as Resource.Success).data)
    }
}
