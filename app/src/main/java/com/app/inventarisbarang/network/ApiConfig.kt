package com.app.inventarisbarang.network

import android.content.Context
import com.app.inventarisbarang.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {

    // Default URL untuk Emulator Android yang mengakses Laravel di localhost komputer host.
    // Catatan: Jika menguji aplikasi di HP fisik nyata, ganti 10.0.2.2 dengan alamat IP Wi-Fi
    // laptop/server Laravel Anda (misalnya: "http://192.168.1.100:8000/api/").
    const val BASE_URL = "http://192.168.0.181:8000/api/"

    fun getApiService(context: Context): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val sessionManager = SessionManager.getInstance(context)
            val token = sessionManager.getToken()

            val requestBuilder = chain.request().newBuilder()
                .addHeader("Accept", "application/json")

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
