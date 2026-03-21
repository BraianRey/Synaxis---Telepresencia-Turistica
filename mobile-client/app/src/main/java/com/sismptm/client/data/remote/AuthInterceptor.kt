package com.sismptm.client.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        
        // Aquí se añadirá el JWT en el futuro
        // val token = getToken() 
        // if (token != null) {
        //     requestBuilder.addHeader("Authorization", "Bearer $token")
        // }
        
        return chain.proceed(requestBuilder.build())
    }
}
