package com.weather.core.domain

import com.weather.core.model.HourlyForecast
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

class GetHourlyForecastUseCaseTest {

    private val repo: WeatherRepository = mockk()
    private val syncWeather: SyncWeatherUseCase = mockk()
    private lateinit var useCase: GetHourlyForecastUseCase

    private val cityName = "Taipei"

    private val cached = listOf(
        HourlyForecast(1000L, 28.0, "01d", "Clear")
    )

    private val updated = listOf(
        HourlyForecast(2000L, 30.0, "02d", "Partly cloudy")
    )

    @Before
    fun setUp() {
        useCase = GetHourlyForecastUseCase(repo, syncWeather)
    }

    @Test
    fun `cache hit then sync success emits cached first then updated`() = runTest {
        val forecastFlow = MutableStateFlow(cached)
        every { repo.observeHourlyForecasts(cityName) } returns forecastFlow
        coEvery { syncWeather(cityName) } coAnswers {
            forecastFlow.value = updated
            Resource.Success(Unit)
        }

        val emissions = useCase(cityName).take(2).toList()

        assertTrue(emissions[0] is Resource.Success)
        assertEquals(cached, (emissions[0] as Resource.Success).data)
        assertTrue(emissions[1] is Resource.Success)
        assertEquals(updated, (emissions[1] as Resource.Success).data)
    }

    @Test
    fun `no cache and sync failure emits Loading then Error`() = runTest {
        val forecastFlow = MutableStateFlow<List<HourlyForecast>>(emptyList())
        every { repo.observeHourlyForecasts(cityName) } returns forecastFlow
        coEvery { syncWeather(cityName) } returns Resource.Error("fail", throwable = IOException())

        val emissions = useCase(cityName).take(2).toList()

        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Error)
    }

    @Test
    fun `cache hit then sync failure keeps showing cached data`() = runTest {
        val forecastFlow = MutableStateFlow(cached)
        every { repo.observeHourlyForecasts(cityName) } returns forecastFlow
        coEvery { syncWeather(cityName) } returns Resource.Error("fail", throwable = IOException())

        val emissions = useCase(cityName).take(1).toList()

        assertTrue(emissions[0] is Resource.Success)
        assertEquals(cached, (emissions[0] as Resource.Success).data)
    }
}
