package com.sismptm.client.utils

import com.sismptm.client.BuildConfig

object NetworkConfig {
    private const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8080/"

    // Usa BASE_URL_API si viene configurada; en local cae a localhost del emulador.
    val BASE_URL: String = BuildConfig.BASE_URL_API.takeIf { it.isNotBlank() } ?: LOCAL_EMULATOR_URL
    val KEYCLOAK_URL: String = BuildConfig.BASE_URL_KEYCLOAK

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
