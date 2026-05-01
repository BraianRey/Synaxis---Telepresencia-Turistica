package com.sismptm.client.domain.model

/**
 * Represents a travel destination in the home screen.
 */
data class Destination(
    val id: Int,
    val city: String,
    val country: String,
    val placeName: String,
    val activePartners: Int,
    val imageUrl: String = ""
)

/**
 * Represents a marker on the map indicating available guides.
 */
data class MapPin(
    val id: Int,
    val city: String,
    val activeGuides: Int,
    val lat: Float,
    val lng: Float
)

/**
 * UI State for the Home screen.
 */
data class HomeUiState(
    val userName: String = "",
    val destinations: List<Destination> = emptyList(),
    val mapPins: List<MapPin> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
