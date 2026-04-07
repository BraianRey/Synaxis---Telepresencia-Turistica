package com.sismptm.client.data.remote

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
        val token = TokenManager.getAccessToken()
        val request = if (token.isNotBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
