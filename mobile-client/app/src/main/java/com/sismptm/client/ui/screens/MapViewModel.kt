package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import com.sismptm.client.data.remote.MapLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MapViewModel : ViewModel() {

    private val _selectedLocation = MutableStateFlow<MapLocation?>(null)
    val selectedLocation: StateFlow<MapLocation?> = _selectedLocation

    fun onLocationSelected(lat: Double, lon: Double) {
        _selectedLocation.value = MapLocation(lat, lon)
    }

    fun clearLocation() {
        _selectedLocation.value = null
    }
}