package com.weather.core.data.repository

import android.content.Context
import android.location.Location
import com.google.android.gms.common.ConnectionResult
import com.weather.core.model.Coordinates
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LocationRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val fusedLocationClient: FusedLocationProviderClient = mockk()

    private val repository = LocationRepositoryImpl(context, fusedLocationClient)

    @Before
    fun setUp() {
        mockkStatic(GoogleApiAvailability::class)
        val mockAvailability = mockk<GoogleApiAvailability>()
        every { GoogleApiAvailability.getInstance() } returns mockAvailability
        every { mockAvailability.isGooglePlayServicesAvailable(any()) } returns ConnectionResult.SUCCESS
    }

    @After
    fun tearDown() {
        unmockkStatic(GoogleApiAvailability::class)
    }

    @Test
    fun `returns null when both lastLocation and getCurrentLocation are null`() = runTest {
        mockLastLocation(null)
        mockGetCurrentLocation(null)

        assertNull(repository.getCurrentLocation())
    }

    @Test
    fun `returns coordinates when lastLocation has a fix`() = runTest {
        val location = mockk<Location> {
            every { latitude } returns 25.04
            every { longitude } returns 121.56
        }
        mockLastLocation(location)

        assertEquals(Coordinates(25.04, 121.56), repository.getCurrentLocation())
    }

    @Test
    fun `falls back to getCurrentLocation when lastLocation cache is empty`() = runTest {
        val location = mockk<Location> {
            every { latitude } returns 25.04
            every { longitude } returns 121.56
        }
        mockLastLocation(null)
        mockGetCurrentLocation(location)

        assertEquals(Coordinates(25.04, 121.56), repository.getCurrentLocation())
    }

    @Test
    fun `returns null when play services unavailable and no native location`() = runTest {
        every {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(any())
        } returns ConnectionResult.SERVICE_MISSING
        val locationManager = mockk<android.location.LocationManager> {
            every { getLastKnownLocation(any()) } returns null
            every { isProviderEnabled(any()) } returns false
        }
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager

        assertNull(repository.getCurrentLocation())
    }

    // --- helpers ---

    private fun mockLastLocation(location: Location?) {
        val task = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.lastLocation } returns task
        every { task.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } answers {
            firstArg<OnSuccessListener<Location?>>().onSuccess(location)
            task
        }
    }

    private fun mockGetCurrentLocation(location: Location?) {
        val task = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.getCurrentLocation(any<Int>(), any()) } returns task
        every { task.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } answers {
            firstArg<OnSuccessListener<Location?>>().onSuccess(location)
            task
        }
    }
}
