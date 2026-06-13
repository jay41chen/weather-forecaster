package com.weather.core.domain

import com.weather.core.model.DailyForecast
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
import java.time.LocalDate

class GetDailyForecastUseCaseTest {

    private val repo: WeatherRepository = mockk()
    private val syncWeather: SyncWeatherUseCase = mockk()
    private lateinit var useCase: GetDailyForecastUseCase

    private val cityName = "Taipei"

    private val cached = listOf(
        DailyForecast(LocalDate.of(2026, 6, 13), 30.0, 22.0, "Cloudy", "03d", 70, 2.0)
    )

    private val updated = listOf(
        DailyForecast(LocalDate.of(2026, 6, 13), 32.0, 24.0, "Clear", "01d", 55, 3.0)
    )

    @Before
    fun setUp() {
        useCase = GetDailyForecastUseCase(repo, syncWeather)
    }

    @Test
    fun `cache hit then sync success emits cached first then updated`() = runTest {
        val forecastFlow = MutableStateFlow(cached)
        every { repo.observeDailyForecasts(cityName) } returns forecastFlow
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
        val forecastFlow = MutableStateFlow<List<DailyForecast>>(emptyList())
        every { repo.observeDailyForecasts(cityName) } returns forecastFlow
        coEvery { syncWeather(cityName) } returns Resource.Error("fail", throwable = IOException())

        val emissions = useCase(cityName).take(2).toList()

        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Error)
    }

    @Test
    fun `cache hit then sync failure keeps showing cached data`() = runTest {
        val forecastFlow = MutableStateFlow(cached)
        every { repo.observeDailyForecasts(cityName) } returns forecastFlow
        coEvery { syncWeather(cityName) } returns Resource.Error("fail", throwable = IOException())

        val emissions = useCase(cityName).take(1).toList()

        assertTrue(emissions[0] is Resource.Success)
        assertEquals(cached, (emissions[0] as Resource.Success).data)
    }
}
