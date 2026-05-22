package com.sismptm.partner.data.remote.api.dto

/**
 * Response DTO for a rating.
 */
data class RatingResponse(
    val id: Long,
    val serviceId: Long,
    val clientId: Long,
    val partnerId: Long,
    val score: Int,
    val comment: String?,
    val createdAt: String?
)
