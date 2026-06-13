package com.weather.core.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.weather.core.model.Coordinates
import com.weather.core.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.resume

class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationRepository {

    override suspend fun getCurrentLocation(): Coordinates? = try {
        withTimeout(5_000L) { getLastLocation() }?.let { Coordinates(it.latitude, it.longitude) }
    } catch (e: TimeoutCancellationException) {
        null
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Location? =
        if (isPlayServicesAvailable()) getFusedLocation() else getNativeLocation()

    private fun isPlayServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    @SuppressLint("MissingPermission")
    private suspend fun getFusedLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(location)
                    } else {
                        val cts = CancellationTokenSource()
                        cont.invokeOnCancellation { cts.cancel() }
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cts.token
                        )
                            .addOnSuccessListener { cont.takeIf { it.isActive }?.resume(it) }
                            .addOnFailureListener { cont.takeIf { it.isActive }?.resume(null) }
                            .addOnCanceledListener { cont.takeIf { it.isActive }?.resume(null) }
                    }
                }
                .addOnFailureListener { cont.resume(null) }
                .addOnCanceledListener { cont.resume(null) }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getNativeLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try cached fix first — no battery cost
        val cached = sequenceOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .firstOrNull()
        if (cached != null) return cached

        // Request a fresh fix — NETWORK_PROVIDER ≈ BALANCED_ACCURACY, GPS as fallback
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }

        return suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    cont.takeIf { it.isActive }?.resume(location)
                }
                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    cont.takeIf { it.isActive }?.resume(null)
                }
            }
            cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
            @Suppress("DEPRECATION")
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }
}
