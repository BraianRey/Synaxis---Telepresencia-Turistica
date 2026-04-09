package com.sismptm.partner.utils
import com.sismptm.partner.BuildConfig

object NetworkConfig {
    const val CONNECT_TIMEOUT = 60L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L
    /**
     * Base URL for REST API calls.
     */
    const val BASE_URL = BuildConfig.BASE_URL_API

    /**
     * WebSocket URL for WebRTC Signaling.
     * Uses the same host as BASE_URL but with ws protocol and specific port.
     */
    const val SIGNALING_URL = BuildConfig.BASE_WEBRTC

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
