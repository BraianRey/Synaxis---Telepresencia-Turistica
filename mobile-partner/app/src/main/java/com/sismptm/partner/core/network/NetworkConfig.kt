package com.sismptm.partner.core.network

import com.sismptm.partner.BuildConfig

/**
 * Configuration for network timeouts and base URL resolution.
 */
object NetworkConfig {
    private const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8080/"

    val BASE_URL: String = BuildConfig.BASE_URL_API.takeIf { it.isNotBlank() } ?: LOCAL_EMULATOR_URL

    const val CONNECT_TIMEOUT = 60L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L
}
