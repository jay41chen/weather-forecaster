package com.weather.core.data.repository

import com.weather.core.data.local.dao.WeatherDao
import com.weather.core.data.local.entity.CurrentWeatherEntity
import com.weather.core.logging.LogPort
import com.weather.core.logging.LogPortFactory
import com.weather.core.model.CurrentWeather
import com.weather.core.model.DailyForecast
import com.weather.core.model.HourlyForecast
import com.weather.core.model.Resource
import com.weather.core.network.ForecastResult
import com.weather.core.network.WeatherApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class WeatherRepositoryImplTest {

    private val api: WeatherApiService = mockk()
    private val weatherDao: WeatherDao = mockk(relaxUnitFun = true)
    private val logPort: LogPort = mockk(relaxed = true)
    private val logFactory: LogPortFactory = mockk {
        every { create(any()) } returns logPort
    }

    private lateinit var repository: WeatherRepositoryImpl

    private val cityName = "Taipei"

    private val sampleWeather = CurrentWeather(
        cityName = cityName,
        country = "TW",
        temperature = 28.0,
        feelsLike = 30.0,
        description = "Clear",
        iconCode = "01d",
        humidity = 60,
        windSpeed = 3.5,
        pressure = 1013,
        timestamp = 1000L
    )

    private val sampleEntity = CurrentWeatherEntity(
        cityName = cityName,
        country = "TW",
        temperature = 28.0,
        feelsLike = 30.0,
        description = "Clear",
        iconCode = "01d",
        humidity = 60,
        windSpeed = 3.5,
        pressure = 1013,
        timestamp = 1000L,
        lastUpdated = System.currentTimeMillis()
    )

    private val sampleForecastResult = ForecastResult(
        hourly = emptyList(),
        daily = emptyList()
    )

    @Before
    fun setUp() {
        repository = WeatherRepositoryImpl(api, weatherDao, logFactory)
    }

    // ---------- sync() ----------

    @Test
    fun `sync skips network when cache is fresh (within 30 minutes)`() = runTest {
        // lastUpdated is current time -> cache is fresh
        coEvery { weatherDao.getLastUpdated(cityName) } returns System.currentTimeMillis()

        val result = repository.sync(cityName)

        assertTrue(result is Resource.Success)
        // API should never be called
        coVerify(exactly = 0) { api.getCurrentWeather(any()) }
        coVerify(exactly = 0) { api.getForecast(any()) }
    }

    @Test
    fun `sync fetches from network when cache is stale (older than 30 minutes)`() = runTest {
        // lastUpdated is 31 minutes ago -> cache is stale
        val staleTime = System.currentTimeMillis() - 31 * 60_000L
        coEvery { weatherDao.getLastUpdated(cityName) } returns staleTime
        coEvery { api.getCurrentWeather(cityName) } returns sampleWeather
        coEvery { api.getForecast(cityName) } returns sampleForecastResult

        val result = repository.sync(cityName)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { api.getCurrentWeather(cityName) }
        coVerify(exactly = 1) { api.getForecast(cityName) }
        coVerify(exactly = 1) { weatherDao.insertCurrentWeather(any()) }
    }

    @Test
    fun `sync fetches from network when no cache exists (lastUpdated is null)`() = runTest {
        coEvery { weatherDao.getLastUpdated(cityName) } returns null
        coEvery { api.getCurrentWeather(cityName) } returns sampleWeather
        coEvery { api.getForecast(cityName) } returns sampleForecastResult

        val result = repository.sync(cityName)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { api.getCurrentWeather(cityName) }
        coVerify(exactly = 1) { api.getForecast(cityName) }
    }

    @Test
    fun `sync returns Error when network call fails`() = runTest {
        coEvery { weatherDao.getLastUpdated(cityName) } returns null
        coEvery { api.getCurrentWeather(cityName) } throws IOException("No internet")

        val result = repository.sync(cityName)

        assertTrue(result is Resource.Error)
        assertEquals("No internet", (result as Resource.Error).message)
    }

    @Test
    fun `sync stores current weather and forecasts in Room on success`() = runTest {
        val dailyForecast = DailyForecast(
            date = LocalDate.of(2026, 6, 11),
            maxTemp = 32.0,
            minTemp = 24.0,
            description = "Sunny",
            iconCode = "01d",
            humidity = 55,
            windSpeed = 2.0
        )
        val hourlyForecast = HourlyForecast(
            timestamp = 2000L,
            temperature = 29.0,
            iconCode = "02d",
            description = "Partly cloudy"
        )
        val forecastResult = ForecastResult(
            hourly = listOf(hourlyForecast),
            daily = listOf(dailyForecast)
        )

        coEvery { weatherDao.getLastUpdated(cityName) } returns null
        coEvery { api.getCurrentWeather(cityName) } returns sampleWeather
        coEvery { api.getForecast(cityName) } returns forecastResult

        repository.sync(cityName)

        coVerify(exactly = 1) { weatherDao.insertCurrentWeather(any()) }
        coVerify(exactly = 1) { weatherDao.replaceDailyForecasts(cityName, any()) }
        coVerify(exactly = 1) { weatherDao.replaceHourlyForecasts(cityName, any()) }
    }

    @Test
    fun `concurrent sync calls for same city deduplicate to single network request`() = runTest {
        val latch = CompletableDeferred<Unit>()
        coEvery { weatherDao.getLastUpdated(cityName) } returns null
        coEvery { api.getCurrentWeather(cityName) } coAnswers { latch.await(); sampleWeather }
        coEvery { api.getForecast(cityName) } returns sampleForecastResult

        // Both jobs run eagerly (UnconfinedTestDispatcher) to their first suspension point.
        // job1 creates the inFlight Deferred and suspends on deferred.await().
        // job2 finds the same Deferred already in inFlight and suspends on the same deferred.await().
        val job1 = async { repository.sync(cityName) }
        val job2 = async { repository.sync(cityName) }

        latch.complete(Unit)

        assertTrue(job1.await() is Resource.Success)
        assertTrue(job2.await() is Resource.Success)
        coVerify(exactly = 1) { api.getCurrentWeather(cityName) }
    }

    // ---------- observeCurrentWeather() ----------

    @Test
    fun `observeCurrentWeather maps entity to domain model`() = runTest {
        val entityFlow = MutableStateFlow<CurrentWeatherEntity?>(sampleEntity)
        every { weatherDao.observeCurrentWeather(cityName) } returns entityFlow

        val result = repository.observeCurrentWeather(cityName).first()

        assertEquals(sampleWeather.cityName, result?.cityName)
        assertEquals(sampleWeather.temperature, result?.temperature)
        assertEquals(sampleWeather.description, result?.description)
    }

    @Test
    fun `observeCurrentWeather emits null when no entity in database`() = runTest {
        val entityFlow = MutableStateFlow<CurrentWeatherEntity?>(null)
        every { weatherDao.observeCurrentWeather(cityName) } returns entityFlow

        val result = repository.observeCurrentWeather(cityName).first()

        assertEquals(null, result)
    }
}
