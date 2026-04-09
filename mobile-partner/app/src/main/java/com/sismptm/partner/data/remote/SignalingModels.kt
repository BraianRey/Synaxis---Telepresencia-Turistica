package com.sismptm.partner.data.remote

/**
 * Data classes for Signaling messages exchanged over WebSocket.
 */
data class SignalingMessage(
    val type: String,
    val sdp: String? = null,
    val candidate: IceCandidateModel? = null,
    val senderId: String? = null,
    val targetId: String? = null
)

data class IceCandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String,
    val targetId: String? = null
)
