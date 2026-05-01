package com.sismptm.partner.manager.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.sismptm.partner.data.repository.PartnerRepositoryImpl
import com.sismptm.partner.domain.usecase.location.UpdateLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manager responsible for GPS tracking and reporting location updates to the backend.
 */
object LocationManager {
    private const val TAG = "LocationManager"
    private const val UPDATE_INTERVAL_SECONDS = 30L

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updateLocationUseCase = UpdateLocationUseCase(PartnerRepositoryImpl())

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { handleLocationUpdate(it) }
        }
    }

    fun init(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_SECONDS * 1000
        ).build()

        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d(TAG, "Location tracking started")
    }

    fun stopTracking() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Location tracking stopped")
    }

    @SuppressLint("MissingPermission")
    fun requestSingleUpdate() {
        serviceScope.launch {
            try {
                val location = fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)?.await()
                location?.let { handleLocationUpdate(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting single location update", e)
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        serviceScope.launch {
            try {
                val response = updateLocationUseCase(location.latitude, location.longitude)
                if (response.isSuccessful) {
                    Log.d(TAG, "Location updated on server")
                } else {
                    Log.e(TAG, "Server error updating location: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error updating location", e)
            }
        }
    }
}
