package com.sismptm.partner.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sismptm.partner.data.remote.LocationUpdateRequest
import com.sismptm.partner.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Singleton service to manage GPS location tracking and reporting to the server.
 */
object LocationService {
    private const val TAG = "LocationService"
    private const val UPDATE_INTERVAL_SECONDS = 30L 

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                handleLocationUpdate(location)
            }
        }
    }

    /**
     * Initializes the FusedLocationProviderClient.
     * @param context Application context.
     */
    fun init(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
    }

    /**
     * Starts receiving location updates.
     * Uses balanced power priority to optimize battery consumption.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_SECONDS * 1000
        ).build()

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(TAG, "Location updates started (Battery Optimized)")
    }

    /**
     * Stops receiving location updates.
     */
    fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Location updates stopped")
    }

    /**
     * Processes new location data, logs it, and sends it to the server.
     * @param location The new location received.
     */
    private fun handleLocationUpdate(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        
        Log.d(TAG, "Location Update: Lat: $lat, Lng: $lng")

        serviceScope.launch {
            try {
                val response = RetrofitClient.apiService.updateLocation(
                    LocationUpdateRequest(latitude = lat, longitude = lng)
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "Location sent to server")
                } else {
                    Log.e(TAG, "Server error updating location: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error updating location: ${e.message}")
            }
        }
    }
}
