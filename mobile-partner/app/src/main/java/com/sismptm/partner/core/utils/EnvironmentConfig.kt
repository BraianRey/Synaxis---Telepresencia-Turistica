package com.sismptm.partner.core.utils

/**
 * Environment configuration for development and production stages.
 */
object EnvironmentConfig {

    /**
     * Set to true for physical hardware, false for Android emulators.
     */
    const val IS_PHYSICAL_DEVICE = false

    /**
     * Backend host address resolution.
     */
    val BACKEND_HOST = if (IS_PHYSICAL_DEVICE) {
        "192.168.1.100"
    } else {
        "10.0.2.2"
    }

    const val BACKEND_PORT = 8080
    const val BACKEND_PROTOCOL = "http"

    val BACKEND_URL: String
        get() = "$BACKEND_PROTOCOL://$BACKEND_HOST:$BACKEND_PORT/"

    val IS_DEBUG: Boolean = true
    val ENABLE_NETWORK_LOGGING: Boolean = IS_DEBUG
}
