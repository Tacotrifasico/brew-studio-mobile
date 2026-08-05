package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClientProvider {
    private const val TAG = "SupabaseClientProvider"

    // Safe retrieval of generated Env constants with fallback checks
    val supabaseUrl: String by lazy {
        try {
            val url = BuildConfig.SUPABASE_URL
            if (url.isNullOrBlank() || url.contains("your-project-id") || url.contains("supabase.co") && url == "https://your-project-id.supabase.co") {
                ""
            } else {
                url
            }
        } catch (e: Exception) {
            Log.w(TAG, "SUPABASE_URL property not found in BuildConfig", e)
            ""
        }
    }

    val supabaseAnonKey: String by lazy {
        try {
            val key = BuildConfig.SUPABASE_ANON_KEY
            if (key.isNullOrBlank() || key.contains("your-key-here")) {
                ""
            } else {
                key
            }
        } catch (e: Exception) {
            Log.w(TAG, "SUPABASE_ANON_KEY property not found in BuildConfig", e)
            ""
        }
    }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    // Holds user JWT token dynamically
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("apikey", supabaseAnonKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            // Attach user session token if active, otherwise use anon key
            val currentToken = authToken ?: supabaseAnonKey
            requestBuilder.header("Authorization", "Bearer $currentToken")

            chain.proceed(requestBuilder.build())
        }

        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: SupabaseApi by lazy {
        // Fallback dummy endpoint to avoid Retrofit crashes on blank URLs
        val baseUrl = if (isConfigured) supabaseUrl else "https://placeholder-domain-for-supabase.supabase.co"
        
        // Ensure trailing slash
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)
    }
}
