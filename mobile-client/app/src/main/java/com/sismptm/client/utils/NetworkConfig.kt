package com.sismptm.client.utils

import com.sismptm.client.BuildConfig

/**
 * Network configuration constants for the application.
 *
 * All URLs are loaded from BuildConfig (gradle build properties from local.properties). This
 * provides a single source of truth for network endpoints and timeout configuration.
 */
object NetworkConfig {
    /** Backend REST API base URL */
    const val BASE_URL = BuildConfig.BASE_URL_API

    /** Keycloak authentication server URL */
    const val KEYCLOAK_URL = BuildConfig.BASE_URL_KEYCLOAK

    /** WebSocket URL for WebRTC signaling server */
    const val WS_SIGNALING_URL = BuildConfig.BASE_WEBRTC

    /** Connection timeout in seconds */
    const val CONNECT_TIMEOUT = 30L
    /** Read timeout in seconds */
    const val READ_TIMEOUT = 30L
    /** Write timeout in seconds */
    const val WRITE_TIMEOUT = 30L
}
