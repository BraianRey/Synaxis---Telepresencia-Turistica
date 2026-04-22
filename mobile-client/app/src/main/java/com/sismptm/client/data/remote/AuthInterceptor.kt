package com.sismptm.client.data.remote

import com.sismptm.client.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds an Authorization header with a Bearer token to every request.
 */
class AuthInterceptor : Interceptor {
    /**
     * Intercepts the request and adds the Authorization header if a token is available.
     * @param chain The interceptor chain.
     * @return The response from the next interceptor in the chain.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")

        // Preferimos la sesión de Sprint 2 y usamos TokenManager como fallback.
        val token = SessionManager.accessToken.ifBlank { TokenManager.getAccessToken() }
        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
