package com.sismptm.partner.utils

/**
 * Configuración de ambiente para desarrollo y producción.
 * Actualiza este archivo según el ambiente en el que estés trabajando.
 */
object EnvironmentConfig {

    /**
     * Set to true cuando estés usando un dispositivo físico
     * Set to false cuando estés usando un emulador Android
     */
    const val IS_PHYSICAL_DEVICE = false

    /**
     * Para Emulador: 10.0.2.2 apunta a localhost en tu máquina host
     * Para Dispositivo Físico: Usar la IP de tu máquina en la red local
     * Ejemplo: "192.168.1.100"
     */
    val BACKEND_HOST = if (IS_PHYSICAL_DEVICE) {
        // Reemplaza esto con la IP de tu máquina
        "192.168.1.100"
    } else {
        // Para emulador
        "10.0.2.2"
    }

    const val BACKEND_PORT = 8080

    const val BACKEND_PROTOCOL = "http" // "http" para desarrollo, "https" para producción

    val BACKEND_URL: String
        get() = "$BACKEND_PROTOCOL://$BACKEND_HOST:$BACKEND_PORT/"

    // BuildType (puedes usar BuildConfig.BUILD_TYPE si está disponible)
    val IS_DEBUG: Boolean = true

    // Logging
    val ENABLE_NETWORK_LOGGING: Boolean = IS_DEBUG
}

