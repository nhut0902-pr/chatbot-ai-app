package com.example.services

import android.util.Log
import com.example.config.AppConfig
import com.example.models.ChatCompletionRequest
import com.example.models.ChatCompletionResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface NvidiaApi {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") token: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

object NvidiaService {
    private const val TAG = "NvidiaService"
    private const val BASE_URL = "https://integrate.api.nvidia.com/v1/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: NvidiaApi = retrofit.create(NvidiaApi::class.java)

    suspend fun fetchCompletion(
        request: ChatCompletionRequest,
        maxRetries: Int = 3
    ): ChatCompletionResponse {
        val apiKey = AppConfig.nvidiaKey
        val authHeader = "Bearer $apiKey"

        return retryWithBackoff(times = maxRetries) {
            Log.d(TAG, "Requesting NVIDIA completion for model ${request.model}")
            api.getChatCompletion(authHeader, request)
        }
    }

    private suspend fun <T> retryWithBackoff(
        times: Int,
        initialDelayMillis: Long = 1000,
        factor: Double = 2.0,
        maxDelayMillis: Long = 8000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "NVIDIA attempt ${attempt + 1} of $times failed: ${e.message}")
                if (e is retrofit2.HttpException) {
                    val code = e.code()
                    if (code == 401 || code == 403) {
                        throw e
                    }
                }
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
        }
        return block()
    }
}
