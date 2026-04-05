package com.sismptm.client.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        requestBuilder.apply {
            addHeader("Accept", "application/json")
            addHeader("Content-Type", "application/json")
            // Aquí se añadirá el JWT en el futuro
            // val token = getToken()
            // if (token != null) {
            //     addHeader("Authorization", "Bearer $token")
            // }
        }

        return chain.proceed(requestBuilder.build())
    }
}
