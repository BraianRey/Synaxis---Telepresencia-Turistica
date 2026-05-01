package com.sismptm.client.core.network

import android.util.Log
import com.sismptm.client.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds an Authorization header with a Bearer token to every request
 * if a session is active.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")

        val token = SessionManager.accessToken
        val url = chain.request().url.toString()
        val method = chain.request().method

        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d("AuthInterceptor", "Token attached to $method $url")
        } else {
            Log.w("AuthInterceptor", "No active session token for $method $url")
        }

        return chain.proceed(requestBuilder.build())
    }
}
