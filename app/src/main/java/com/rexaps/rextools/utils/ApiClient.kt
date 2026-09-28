package com.rexaps.rextools.utils

import com.rexaps.rexmusic.YtPlayResponse
import com.rexaps.rextools.PinterestResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface PinterestApiService {
    @GET("faa/pinterest")
    suspend fun searchPinterest(@Query("q") query: String): PinterestResponse
}

interface RexMusicApiService {
    @GET("faa/ytplay")
    suspend fun play(@Query("query") query: String): YtPlayResponse
}

object ApiClient {
    private const val BASE_URL = "https://api-faa.my.id/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val pinterest: PinterestApiService by lazy {
        retrofit.create(PinterestApiService::class.java)
    }

    val rexMusic: RexMusicApiService by lazy {
        retrofit.create(RexMusicApiService::class.java)
    }
}
