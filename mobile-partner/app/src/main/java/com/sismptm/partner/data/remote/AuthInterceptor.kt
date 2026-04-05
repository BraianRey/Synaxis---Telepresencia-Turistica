package com.sismptm.partner.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Agregar headers necesarios
        requestBuilder.apply {
            addHeader("Accept", "application/json")
            addHeader("Content-Type", "application/json")
            // Si tienes un token guardado, aquí lo agregarías
            // val token = getStoredToken() // Implementar según tu storage
            // if (token.isNotEmpty()) {
            //     addHeader("Authorization", "Bearer $token")
            // }
        }

        return chain.proceed(requestBuilder.build())
    }
}
