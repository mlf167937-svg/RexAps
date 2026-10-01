package com.rexaps.rextools.utils

import com.rexaps.rexmusic.SpotifyDownloadResponse
import com.rexaps.rexmusic.SpotifySearchResponse
import com.rexaps.rextools.PinterestResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.rexaps.rextools.SsWebResponse
import com.rexaps.rexmusic.LyricsResponse

interface PinterestApiService {
    @GET("faa/pinterest")
    suspend fun searchPinterest(@Query("q") query: String): PinterestResponse
}

interface RexMusicApiService {
    @GET("search/spotify")
    suspend fun searchSpotify(@Query("q") query: String): SpotifySearchResponse

    @GET("downloader/spotify")
    suspend fun downloadSpotify(@Query("url") url: String): SpotifyDownloadResponse

    @GET("search/lyrics")
    suspend fun searchLyrics(@Query("q") query: String): LyricsResponse
}

interface SsWebApiService {
    @GET("tools/ssweb")
    suspend fun capture(
        @Query("url") url: String,
        @Query("width") width: Int,
        @Query("height") height: Int,
        @Query("device_scale") scale: Int
    ): SsWebResponse
}

object ApiClient {
    private const val PINTEREST_BASE = "https://api-faa.my.id/"
    private const val NEXRAY_BASE = "https://api.nexray.eu.cc/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val pinterestRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(PINTEREST_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val rexMusicRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NEXRAY_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val pinterest: PinterestApiService by lazy {
        pinterestRetrofit.create(PinterestApiService::class.java)
    }

    val rexMusic: RexMusicApiService by lazy {
        rexMusicRetrofit.create(RexMusicApiService::class.java)
    }
    val ssweb: SsWebApiService by lazy {
        rexMusicRetrofit.create(SsWebApiService::class.java)
    }
}
