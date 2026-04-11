package com.sismptm.client.ui.screens

data class Destination(
    val id: Int,
    val city: String,
    val country: String,
    val placeName: String,
    val activePartners: Int,
    val imageUrl: String = ""
)

data class MapPin(
    val id: Int,
    val city: String,
    val activeGuides: Int,
    val lat: Float,
    val lng: Float
)

data class HomeUiState(
    val userName: String = "",
    val destinations: List<Destination> = emptyList(),
    val mapPins: List<MapPin> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
