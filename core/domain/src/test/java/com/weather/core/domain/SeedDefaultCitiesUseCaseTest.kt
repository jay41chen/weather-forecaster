package com.weather.core.domain

import com.weather.core.model.City
import com.weather.core.repository.CityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SeedDefaultCitiesUseCaseTest {

    private val repo: CityRepository = mockk(relaxed = true)
    private lateinit var useCase: SeedDefaultCitiesUseCase

    @Before
    fun setUp() {
        useCase = SeedDefaultCitiesUseCase(repo)
    }

    @Test
    fun `seeds cities and selects London when repo is empty`() = runTest {
        coEvery { repo.count() } returns 0

        useCase()

        coVerify(exactly = 11) { repo.saveCity(any()) }
        coVerify { repo.selectCity("London") }
    }

    @Test
    fun `skips seeding when repo already has cities`() = runTest {
        coEvery { repo.count() } returns 5

        useCase()

        coVerify(exactly = 0) { repo.saveCity(any()) }
        coVerify(exactly = 0) { repo.selectCity(any()) }
    }
}
