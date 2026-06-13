package com.weather.feature.citylist

import com.weather.core.config.FeatureFlag
import com.weather.core.config.FeatureToggleMockAdapter
import com.weather.core.domain.GetSavedCitiesUseCase
import com.weather.core.domain.GetSelectedCityUseCase
import com.weather.core.domain.RemoveCityUseCase
import com.weather.core.domain.SaveCityUseCase
import com.weather.core.domain.SearchCitiesUseCase
import com.weather.core.domain.SelectCityUseCase
import com.weather.core.model.City
import com.weather.core.model.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CityListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getSavedCities: GetSavedCitiesUseCase = mockk()
    private val getSelectedCity: GetSelectedCityUseCase = mockk()
    private val searchCities: SearchCitiesUseCase = mockk()
    private val saveCity: SaveCityUseCase = mockk(relaxed = true)
    private val removeCity: RemoveCityUseCase = mockk(relaxed = true)
    private val selectCity: SelectCityUseCase = mockk(relaxed = true)
    private val featureToggle = FeatureToggleMockAdapter()

    private val savedCitiesFlow = MutableStateFlow<List<City>>(emptyList())
    private val selectedCityFlow = MutableStateFlow<String?>(null)

    private val tokyo = City("Tokyo", "JP", null, 35.68, 139.69)
    private val taipei = City("Taipei", "TW", null, 25.03, 121.56)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSavedCities() } returns savedCitiesFlow
        every { getSelectedCity() } returns selectedCityFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CityListViewModel {
        return CityListViewModel(
            getSavedCities = getSavedCities,
            getSelectedCity = getSelectedCity,
            searchCities = searchCities,
            saveCity = saveCity,
            removeCity = removeCity,
            selectCity = selectCity,
            featureToggle = featureToggle
        )
    }

    @Test
    fun `saved cities are reflected in uiState`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        savedCitiesFlow.value = listOf(tokyo, taipei)
        advanceUntilIdle()

        assertEquals(listOf(tokyo, taipei), vm.uiState.value.savedCities)
    }

    @Test
    fun `selected city name is reflected in uiState`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        selectedCityFlow.value = "Tokyo"
        advanceUntilIdle()

        assertEquals("Tokyo", vm.uiState.value.selectedCityName)
    }

    @Test
    fun `onCitySelect calls selectCity and emits NavigateBack`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onCitySelect(tokyo)
        advanceUntilIdle()

        coVerify { selectCity("Tokyo") }
        assertEquals(CityListEvent.NavigateBack, vm.events.first())
    }

    @Test
    fun `onCityAdd calls saveCity`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onCityAdd(tokyo)
        advanceUntilIdle()

        coVerify { saveCity(tokyo) }
    }

    @Test
    fun `onCityRemove calls removeCity`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onCityRemove(tokyo)
        advanceUntilIdle()

        coVerify { removeCity(tokyo) }
    }

    @Test
    fun `search query triggers search after debounce`() = runTest(testDispatcher) {
        coEvery { searchCities("Tok") } returns Resource.Success(listOf(tokyo))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSearchQueryChange("Tok")
        advanceUntilIdle()

        assertEquals(listOf(tokyo), vm.uiState.value.searchResults)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `blank search query clears results`() = runTest(testDispatcher) {
        coEvery { searchCities("Tok") } returns Resource.Success(listOf(tokyo))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSearchQueryChange("Tok")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.searchResults.size)

        vm.onSearchQueryChange("")
        advanceUntilIdle()

        assertEquals(emptyList<City>(), vm.uiState.value.searchResults)
    }

    @Test
    fun `showSearch reflects feature flag`() = runTest(testDispatcher) {
        featureToggle.setFlag(FeatureFlag.CITY_SEARCH_ENABLED, false)
        val vm = createViewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showSearch)

        featureToggle.setFlag(FeatureFlag.CITY_SEARCH_ENABLED, true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showSearch)
    }
}
