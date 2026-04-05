package com.sismptm.client.utils

object NetworkConfig {
    // Si usas el emulador de Android, 10.0.2.2 apunta al localhost de tu máquina host.
    // El backend corre en el puerto 8080 (ver application.yaml)
    const val BASE_URL = "http://10.0.2.2:8080/"
    const val CONNECT_TIMEOUT = 60L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L
}
