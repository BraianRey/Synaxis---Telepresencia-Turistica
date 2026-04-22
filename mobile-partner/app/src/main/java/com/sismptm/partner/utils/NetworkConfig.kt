package com.sismptm.partner.utils

import com.sismptm.partner.BuildConfig

object NetworkConfig {
    /**
     * Base URL for the backend API, loaded from local.properties (BASE_URL_API).
     */
    val BASE_URL: String = BuildConfig.BASE_URL_API

    const val CONNECT_TIMEOUT = 60L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L
}
