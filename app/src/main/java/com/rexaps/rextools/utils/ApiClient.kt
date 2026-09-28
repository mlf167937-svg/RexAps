package com.rexaps.rextools.utils

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.rexaps.rextools.PinterestResponse
import java.util.concurrent.TimeUnit

interface PinterestApiService {
    @GET("faa/pinterest")
    suspend fun searchPinterest(
        @Query("q") query: String
    ): PinterestResponse
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

    val pinterest: PinterestApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PinterestApiService::class.java)
    }
}
