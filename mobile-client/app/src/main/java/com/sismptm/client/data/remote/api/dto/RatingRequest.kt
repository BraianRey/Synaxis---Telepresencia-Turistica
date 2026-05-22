package com.sismptm.client.data.remote.api.dto

/**
 * Request body for creating a new rating.
 */
data class RatingRequest(
    val serviceId: Long,
    val score: Int,
    val comment: String? = null
)
