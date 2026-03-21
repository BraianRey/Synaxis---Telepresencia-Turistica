package com.sismptm.client.data.remote

import com.sismptm.client.utils.NetworkConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = AuthInterceptor()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
