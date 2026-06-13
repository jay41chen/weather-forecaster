package com.weather.core.domain

import com.weather.core.logging.LogPort
import com.weather.core.logging.LogPortFactory
import com.weather.core.model.Resource
import com.weather.core.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SyncWeatherUseCaseTest {

    private val repo: WeatherRepository = mockk()
    private val logFactory: LogPortFactory = mockk()
    private val log: LogPort = mockk(relaxed = true)
    private lateinit var useCase: SyncWeatherUseCase

    private val cityName = "Tokyo"

    @Before
    fun setUp() {
        every { logFactory.create(any()) } returns log
        useCase = SyncWeatherUseCase(repo, logFactory)
    }

    @Test
    fun `first call syncs from repo`() = runTest {
        coEvery { repo.sync(cityName) } returns Resource.Success(Unit)

        val result = useCase(cityName)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { repo.sync(cityName) }
    }

    @Test
    fun `second call within TTL returns cache hit without calling repo`() = runTest {
        coEvery { repo.sync(cityName) } returns Resource.Success(Unit)

        useCase(cityName)
        val result = useCase(cityName)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { repo.sync(cityName) }
    }

    @Test
    fun `sync failure does not update lastSynced so next call retries`() = runTest {
        coEvery { repo.sync(cityName) } returnsMany listOf(
            Resource.Error("Network error", throwable = IOException()),
            Resource.Success(Unit)
        )

        val first = useCase(cityName)
        assertTrue(first is Resource.Error)

        val second = useCase(cityName)
        assertTrue(second is Resource.Success)
        coVerify(exactly = 2) { repo.sync(cityName) }
    }

    @Test
    fun `different cities sync independently`() = runTest {
        coEvery { repo.sync("Tokyo") } returns Resource.Success(Unit)
        coEvery { repo.sync("Taipei") } returns Resource.Success(Unit)

        useCase("Tokyo")
        useCase("Taipei")

        coVerify(exactly = 1) { repo.sync("Tokyo") }
        coVerify(exactly = 1) { repo.sync("Taipei") }
    }

    @Test
    fun `concurrent calls for same city dedup via mutex`() = runTest {
        coEvery { repo.sync(cityName) } returns Resource.Success(Unit)

        val jobs = (1..3).map { launch { useCase(cityName) } }
        jobs.forEach { it.join() }

        coVerify(exactly = 1) { repo.sync(cityName) }
    }
}
