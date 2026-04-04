package com.sismptm.client.utils
import com.sismptm.client.BuildConfig

object NetworkConfig {
    // BuildConfig se genera automáticamente al compilar el proyecto, y contiene las constantes definidas en el archivo build.gradle
    const val BASE_URL = BuildConfig.BASE_URL_API
    const val KEYCLOAK_URL = BuildConfig.BASE_URL_KEYCLOAK
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
