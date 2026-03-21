package com.sismptm.client.utils

object NetworkConfig {
    // Si usas el emulador de Android, 10.0.2.2 apunta al localhost de tu máquina host.
    const val BASE_URL = "http://10.0.2.2:8080"
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
