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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `sync fetches from API and persists to Room`() = runTest {
        coEvery { api.getCurrentWeather(cityName) } returns sampleWeather
        coEvery { api.getForecast(cityName) } returns sampleForecastResult

        val result = repository.sync(cityName)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { api.getCurrentWeather(cityName) }
        coVerify(exactly = 1) { api.getForecast(cityName) }
        coVerify(exactly = 1) { weatherDao.insertCurrentWeather(any()) }
    }

    @Test
    fun `sync returns Error when network call fails`() = runTest {
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

        coEvery { api.getCurrentWeather(cityName) } returns sampleWeather
        coEvery { api.getForecast(cityName) } returns forecastResult

        repository.sync(cityName)

        coVerify(exactly = 1) { weatherDao.insertCurrentWeather(any()) }
        coVerify(exactly = 1) { weatherDao.replaceDailyForecasts(cityName, any()) }
        coVerify(exactly = 1) { weatherDao.replaceHourlyForecasts(cityName, any()) }
    }

    // ---------- getCurrentWeatherByCoords() ----------

    @Test
    fun `getCurrentWeatherByCoords returns weather and persists`() = runTest {
        coEvery { api.getCurrentWeatherByCoords(25.0, 121.5) } returns sampleWeather

        val result = repository.getCurrentWeatherByCoords(25.0, 121.5)

        assertEquals(sampleWeather, result)
        coVerify { weatherDao.insertCurrentWeather(any()) }
    }

    @Test
    fun `getCurrentWeatherByCoords returns null on exception`() = runTest {
        coEvery { api.getCurrentWeatherByCoords(any(), any()) } throws IOException("fail")

        val result = repository.getCurrentWeatherByCoords(25.0, 121.5)

        assertNull(result)
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

        assertNull(result)
    }
}
