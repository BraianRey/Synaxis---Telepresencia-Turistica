package com.sismptm.partner.core.network

import com.sismptm.partner.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds the Authorization header with a Bearer token to every request.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")

        val token = SessionManager.accessToken
        if (token.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
