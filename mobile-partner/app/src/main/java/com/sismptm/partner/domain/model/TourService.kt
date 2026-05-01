package com.sismptm.partner.domain.model

/**
 * Domain model representing a tour service, decoupled from API DTOs.
 */
data class TourService(
    val id: Long,
    val clientId: Long,
    val clientName: String,
    val partnerId: Long?,
    val location: String,
    val longitude: Double,
    val latitude: Double,
    val agreedHours: Int,
    val hourlyRate: Double,
    val status: ServiceStatus,
    val requestedAt: String?
)

enum class ServiceStatus {
    REQUESTED, ACCEPTED, STARTED, COMPLETED, CANCELLED, UNKNOWN
}
