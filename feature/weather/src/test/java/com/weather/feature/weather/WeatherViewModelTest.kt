package com.weather.feature.weather

import com.weather.core.config.FeatureFlag
import com.weather.core.config.FeatureToggleMockAdapter
import com.weather.core.domain.GetCurrentWeatherUseCase
import com.weather.core.domain.GetDailyForecastUseCase
import com.weather.core.domain.GetHourlyForecastUseCase
import com.weather.core.domain.GetSelectedCityUseCase
import com.weather.core.domain.RefreshWeatherUseCase
import com.weather.core.model.ApiError
import com.weather.core.model.CurrentWeather
import com.weather.core.model.DailyForecast
import com.weather.core.model.HourlyForecast
import com.weather.core.model.Resource
import com.weather.core.model.WeatherAlert
import com.weather.core.repository.WeatherRealtimeService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val getCurrentWeather: GetCurrentWeatherUseCase = mockk()
    private val getDailyForecast: GetDailyForecastUseCase = mockk()
    private val getHourlyForecast: GetHourlyForecastUseCase = mockk()
    private val refreshWeather: RefreshWeatherUseCase = mockk()
    private val getSelectedCity: GetSelectedCityUseCase = mockk()
    private val realtime: WeatherRealtimeService = mockk(relaxed = true)
    private val featureToggle = FeatureToggleMockAdapter()

    private val selectedCity = MutableStateFlow<String?>(null)

    private val weather = CurrentWeather(
        cityName = "Tokyo",
        country = "JP",
        temperature = 22.0,
        feelsLike = 24.0,
        description = "Clear",
        iconCode = "01d",
        humidity = 55,
        windSpeed = 3.0,
        pressure = 1013,
        timestamp = 1000L
    )

    private val dailyForecast = DailyForecast(
        date = LocalDate.of(2026, 6, 13),
        maxTemp = 28.0,
        minTemp = 18.0,
        description = "Sunny",
        iconCode = "01d",
        humidity = 40,
        windSpeed = 2.5
    )

    private val hourlyForecast = HourlyForecast(
        timestamp = 1000L,
        temperature = 22.0,
        iconCode = "01d",
        description = "Clear"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSelectedCity() } returns selectedCity
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): WeatherViewModel {
        return WeatherViewModel(
            getCurrentWeather = getCurrentWeather,
            getDailyForecast = getDailyForecast,
            getHourlyForecast = getHourlyForecast,
            refreshWeather = refreshWeather,
            getSelectedCity = getSelectedCity,
            featureToggle = featureToggle,
            realtime = realtime,
            appScope = testScope
        )
    }

    @Test
    fun `selecting a city loads weather and updates uiState`() = runTest(testDispatcher) {
        every { getCurrentWeather("Tokyo") } returns flowOf(Resource.Success(weather))
        every { getDailyForecast("Tokyo") } returns flowOf(Resource.Success(listOf(dailyForecast)))
        every { getHourlyForecast("Tokyo") } returns flowOf(Resource.Success(listOf(hourlyForecast)))

        val vm = createViewModel()
        selectedCity.value = "Tokyo"
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Tokyo", state.cityName)
        assertEquals(weather, state.currentWeather)
        assertEquals(listOf(dailyForecast), state.dailyForecasts)
        assertEquals(listOf(hourlyForecast), state.hourlyForecasts)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `weather error sets error in uiState`() = runTest(testDispatcher) {
        every { getCurrentWeather("Tokyo") } returns flowOf(
            Resource.Error("City not found", ApiError.CityNotFound)
        )
        every { getDailyForecast("Tokyo") } returns flowOf(Resource.Loading)
        every { getHourlyForecast("Tokyo") } returns flowOf(Resource.Loading)

        val vm = createViewModel()
        selectedCity.value = "Tokyo"
        advanceUntilIdle()

        assertEquals(ApiError.CityNotFound, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `refresh sets isSyncing then clears it`() = runTest(testDispatcher) {
        every { getCurrentWeather("Tokyo") } returns flowOf(Resource.Success(weather))
        every { getDailyForecast("Tokyo") } returns flowOf(Resource.Success(emptyList()))
        every { getHourlyForecast("Tokyo") } returns flowOf(Resource.Success(emptyList()))
        coEvery { refreshWeather("Tokyo") } returns Resource.Success(Unit)

        val vm = createViewModel()
        selectedCity.value = "Tokyo"
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSyncing)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `refresh failure sets error`() = runTest(testDispatcher) {
        every { getCurrentWeather("Tokyo") } returns flowOf(Resource.Success(weather))
        every { getDailyForecast("Tokyo") } returns flowOf(Resource.Success(emptyList()))
        every { getHourlyForecast("Tokyo") } returns flowOf(Resource.Success(emptyList()))
        coEvery { refreshWeather("Tokyo") } returns Resource.Error("fail", ApiError.NetworkUnavailable)

        val vm = createViewModel()
        selectedCity.value = "Tokyo"
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertEquals(ApiError.NetworkUnavailable, vm.uiState.value.error)
    }

    @Test
    fun `refresh with blank city is no-op`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        coVerify(exactly = 0) { refreshWeather(any()) }
    }

    @Test
    fun `dismissError clears error`() = runTest(testDispatcher) {
        every { getCurrentWeather("Tokyo") } returns flowOf(
            Resource.Error("fail", ApiError.ServerError)
        )
        every { getDailyForecast("Tokyo") } returns flowOf(Resource.Loading)
        every { getHourlyForecast("Tokyo") } returns flowOf(Resource.Loading)

        val vm = createViewModel()
        selectedCity.value = "Tokyo"
        advanceUntilIdle()
        assertEquals(ApiError.ServerError, vm.uiState.value.error)

        vm.dismissError()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `dismissAlert clears alertMessage`() = runTest(testDispatcher) {
        val alertFlow = MutableSharedFlow<WeatherAlert>(extraBufferCapacity = 1)
        every { realtime.observeWeatherAlerts() } returns alertFlow
        featureToggle.setFlag(FeatureFlag.WEATHER_ALERTS_ENABLED, true)

        every { getCurrentWeather(any()) } returns flowOf(Resource.Loading)
        every { getDailyForecast(any()) } returns flowOf(Resource.Loading)
        every { getHourlyForecast(any()) } returns flowOf(Resource.Loading)

        val vm = createViewModel()
        advanceUntilIdle()

        alertFlow.emit(WeatherAlert("Tokyo", "storm", "Storm warning", 1000L))
        advanceUntilIdle()
        assertEquals("Storm warning", vm.uiState.value.alertMessage)

        vm.dismissAlert()
        assertNull(vm.uiState.value.alertMessage)
    }

    @Test
    fun `socket connect called when feature enabled`() = runTest(testDispatcher) {
        featureToggle.setFlag(FeatureFlag.SOCKET_IO_ENABLED, true)

        every { getCurrentWeather(any()) } returns flowOf(Resource.Loading)
        every { getDailyForecast(any()) } returns flowOf(Resource.Loading)
        every { getHourlyForecast(any()) } returns flowOf(Resource.Loading)

        createViewModel()
        advanceUntilIdle()

        coVerify { realtime.connect() }
    }

    @Test
    fun `switching city unsubscribes old and subscribes new`() = runTest(testDispatcher) {
        featureToggle.setFlag(FeatureFlag.SOCKET_IO_ENABLED, true)

        every { getCurrentWeather(any()) } returns flowOf(Resource.Success(weather))
        every { getDailyForecast(any()) } returns flowOf(Resource.Success(emptyList()))
        every { getHourlyForecast(any()) } returns flowOf(Resource.Success(emptyList()))

        val vm = createViewModel()
        selectedCity.value = "Tokyo"
        advanceUntilIdle()

        coVerify { realtime.subscribeCities(listOf("Tokyo")) }
        coVerify(exactly = 0) { realtime.unsubscribeCities(any()) }

        selectedCity.value = "Taipei"
        advanceUntilIdle()

        coVerify { realtime.unsubscribeCities(listOf("Tokyo")) }
        coVerify { realtime.subscribeCities(listOf("Taipei")) }
    }

    @Test
    fun `showHourlyForecast reflects feature flag`() = runTest(testDispatcher) {
        val vm = createViewModel()

        featureToggle.setFlag(FeatureFlag.HOURLY_FORECAST_ENABLED, false)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showHourlyForecast)

        featureToggle.setFlag(FeatureFlag.HOURLY_FORECAST_ENABLED, true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showHourlyForecast)
    }
}
