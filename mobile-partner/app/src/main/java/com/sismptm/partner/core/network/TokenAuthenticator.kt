package com.sismptm.partner.core.network

import android.util.Log
import com.google.gson.Gson
import com.sismptm.partner.core.session.SessionManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * OkHttp Authenticator that automatically refreshes the access token
 * when a 401 Unauthorized response is received on the partner side.
 */
class TokenAuthenticator : Authenticator {
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = SessionManager.refreshToken
        if (refreshToken.isBlank()) return null

        synchronized(this) {
            val currentToken = SessionManager.accessToken
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshRequest = Request.Builder()
                .url("${NetworkConfig.BASE_URL}api/auth/refresh")
                .post("{\"refreshToken\":\"$refreshToken\"}".toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val client = OkHttpClient()
            try {
                val refreshResponse = client.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val body = refreshResponse.body?.string()
                    val map = gson.fromJson(body, Map::class.java)
                    val newAccessToken = map["accessToken"] as? String ?: ""
                    val newRefreshToken = map["refreshToken"] as? String ?: ""

                    if (newAccessToken.isNotBlank()) {
                        Log.i("TokenAuthenticator", "Token refreshed successfully")
                        SessionManager.saveSession(
                            token = newAccessToken,
                            refreshTkn = newRefreshToken,
                            id = SessionManager.partnerId,
                            name = SessionManager.partnerName,
                            email = SessionManager.partnerEmail,
                            lang = SessionManager.language,
                            picDirectory = SessionManager.picDirectory
                        )

                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    }
                } else {
                    Log.e("TokenAuthenticator", "Token refresh failed: ${refreshResponse.code}")
                    SessionManager.clearSession()
                }
            } catch (e: IOException) {
                Log.e("TokenAuthenticator", "Network error during token refresh: ${e.message}")
            }
        }
        return null
    }
}
