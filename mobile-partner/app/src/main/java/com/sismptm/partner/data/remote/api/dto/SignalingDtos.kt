package com.sismptm.partner.data.remote.api.dto

/**
 * Data transfer objects for WebRTC signaling.
 */
data class IceCandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String,
    val targetId: String? = null
)
