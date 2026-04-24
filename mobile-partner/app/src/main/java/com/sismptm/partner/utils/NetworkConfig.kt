package com.sismptm.partner.utils

import com.sismptm.partner.BuildConfig

object NetworkConfig {
    private const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8080/"

    /**
     * Base URL for the backend API, loaded from local.properties (BASE_URL_API).
     * Falls back to emulator localhost if not set.
     */
    val BASE_URL: String = BuildConfig.BASE_URL_API.takeIf { it.isNotBlank() } ?: LOCAL_EMULATOR_URL

    const val CONNECT_TIMEOUT = 60L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L
}
