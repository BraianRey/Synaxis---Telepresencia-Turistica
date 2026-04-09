package com.sismptm.client.utils

import com.sismptm.client.BuildConfig

/**
 * Network configuration constants for the application.
 */
object NetworkConfig {
    /** Main Backend API URL */
    const val BASE_URL = BuildConfig.BASE_URL_API

    /** Keycloak Auth URL */
    const val KEYCLOAK_URL = BuildConfig.BASE_URL_KEYCLOAK

    /** WebSocket URL for WebRTC Signaling */
    const val WS_SIGNALING_URL = BuildConfig.BASE_WEBRTC

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
