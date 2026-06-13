package com.weather.core.domain

import com.weather.core.logging.LogPort
import com.weather.core.logging.LogPortFactory
import com.weather.core.model.Coordinates
import com.weather.core.model.CurrentWeather
import com.weather.core.repository.CityRepository
import com.weather.core.repository.LocationRepository
import com.weather.core.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class DetectAndSelectCityUseCaseTest {

    private val locationRepository: LocationRepository = mockk()
    private val weatherRepository: WeatherRepository = mockk()
    private val cityRepository: CityRepository = mockk(relaxed = true)
    private val logFactory: LogPortFactory = mockk {
        every { create(any()) } returns mockk<LogPort>(relaxed = true)
    }

    private lateinit var useCase: DetectAndSelectCityUseCase

    private val coords = Coordinates(25.03, 121.56)

    private val weather = CurrentWeather(
        cityName = "Taipei",
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

    @Before
    fun setUp() {
        useCase = DetectAndSelectCityUseCase(locationRepository, weatherRepository, cityRepository, logFactory)
    }

    @Test
    fun `selects city when location and weather succeed`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns coords
        coEvery { weatherRepository.getCurrentWeatherByCoords(coords.latitude, coords.longitude) } returns weather

        useCase()

        coVerify { cityRepository.selectCity("Taipei") }
    }

    @Test
    fun `returns early when location is null`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns null

        useCase()

        coVerify(exactly = 0) { weatherRepository.getCurrentWeatherByCoords(any(), any()) }
        coVerify(exactly = 0) { cityRepository.selectCity(any()) }
    }

    @Test
    fun `returns early when weather lookup returns null`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns coords
        coEvery { weatherRepository.getCurrentWeatherByCoords(any(), any()) } returns null

        useCase()

        coVerify(exactly = 0) { cityRepository.selectCity(any()) }
    }

    @Test
    fun `catches non-cancellation exceptions`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } throws IOException("GPS fail")

        useCase()

        coVerify(exactly = 0) { cityRepository.selectCity(any()) }
    }

    @Test(expected = CancellationException::class)
    fun `rethrows CancellationException`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } throws CancellationException("cancelled")

        useCase()
    }
}
