package com.sismptm.client.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MapLocation(
    val lat: Double,
    val lon: Double
)

class MapViewModel : ViewModel() {

    private val _selectedLocation = MutableStateFlow<MapLocation?>(null)
    val selectedLocation: StateFlow<MapLocation?> = _selectedLocation

    // ← NEW: to store location description
    private val _locationDescription = MutableStateFlow<String>("")
    val locationDescription: StateFlow<String> = _locationDescription

    // ← NEW: to control whether the description bottom sheet is shown
    private val _showDescriptionSheet = MutableStateFlow<Boolean>(false)
    val showDescriptionSheet: StateFlow<Boolean> = _showDescriptionSheet

    fun onLocationSelected(lat: Double, lon: Double) {
        _selectedLocation.value = MapLocation(lat, lon)
    }

    fun onDescriptionChanged(description: String) {
        _locationDescription.value = description
    }

    fun showDescriptionSheet() {
        _showDescriptionSheet.value = true
    }

    fun hideDescriptionSheet() {
        _showDescriptionSheet.value = false
    }

    fun clearLocation() {
        _selectedLocation.value = null
        _locationDescription.value = ""
        _showDescriptionSheet.value = false
    }
}