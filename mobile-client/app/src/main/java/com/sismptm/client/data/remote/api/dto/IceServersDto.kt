package com.sismptm.client.data.remote.api.dto

/**
 * Data classes for WebRTC ICE and TURN servers configuration.
 */
data class IceServersResponse(
    val iceServers: List<IceServerInfo>
)

data class IceServerInfo(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)
