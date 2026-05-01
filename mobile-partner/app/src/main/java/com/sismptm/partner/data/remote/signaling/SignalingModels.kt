package com.sismptm.partner.data.remote.signaling

import com.sismptm.partner.data.remote.api.dto.IceCandidateModel

/**
 * Data classes for signaling messages exchanged over WebSocket.
 */
data class SignalingMessage(
    val type: String,
    val sdp: String? = null,
    val text: String? = null,
    val candidate: IceCandidateModel? = null,
    val senderId: String? = null,
    val targetId: String? = null
)
