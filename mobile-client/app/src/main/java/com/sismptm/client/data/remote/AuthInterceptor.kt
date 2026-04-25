package com.sismptm.client.data.remote

import android.util.Log
import com.sismptm.client.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds an Authorization header with a Bearer token to every request.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")

        val token = SessionManager.accessToken.ifBlank { TokenManager.getAccessToken() }
        val url = chain.request().url.toString()

        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d("AuthInterceptor", "🔑 Token present (${token.take(20)}...) → ${chain.request().method} $url")
        } else {
            Log.w("AuthInterceptor", "⚠️ NO TOKEN for request → ${chain.request().method} $url")
        }

        return chain.proceed(requestBuilder.build())
    }
}
