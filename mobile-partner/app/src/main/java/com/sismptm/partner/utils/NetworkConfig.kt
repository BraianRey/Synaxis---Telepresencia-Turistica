package com.sismptm.partner.utils

object NetworkConfig {
    // Usar configuración de ambiente para emulador/dispositivo físico.
    val BASE_URL: String = EnvironmentConfig.BACKEND_URL
    const val CONNECT_TIMEOUT = 60L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L
}
