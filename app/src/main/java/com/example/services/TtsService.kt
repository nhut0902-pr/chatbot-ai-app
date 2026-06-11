package com.example.services

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface TtsApi {
    @Streaming
    @GET("tts")
    suspend fun generateTts(
        @Query("text") text: String,
        @Query("voice") voice: String
    ): Response<ResponseBody>
}

object TtsService {
    private const val TAG = "TtsService"
    private const val BASE_URL = "https://tts-voice-ai.onrender.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS // Avoid printing huge audio binary stream body
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    val api: TtsApi = retrofit.create(TtsApi::class.java)

    /**
     * Calls the TTS API to download the voice audio stream and returns the Response containing ResponseBody.
     */
    suspend fun getVoiceAudio(text: String, voice: String): Response<ResponseBody> {
        Log.d(TAG, "Requesting TTS. Text: '$text', Voice: '$voice'")
        return api.generateTts(text, voice)
    }
}
