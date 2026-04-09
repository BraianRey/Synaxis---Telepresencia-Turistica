package com.sismptm.client.utils
import com.sismptm.client.BuildConfig

/**
 * Network configuration constants for the application.
 */
object NetworkConfig {
    private const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8080/"

    /** Main Backend API URL */
    val BASE_URL: String = BuildConfig.BASE_URL_API.takeIf { it.isNotBlank() } ?: LOCAL_EMULATOR_URL
    /** Keycloak Auth URL */
    val KEYCLOAK_URL: String = BuildConfig.BASE_URL_KEYCLOAK

    /** WebSocket URL for WebRTC Signaling */
    const val WS_SIGNALING_URL = BuildConfig.BASE_URL_API

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
