package com.rexaps.rextools.ssweb

import com.rexaps.rextools.utils.ApiClient

object SsWebApi {

    suspend fun capture(
        url: String,
        width: Int = 1080,
        height: Int = 1920,
        scale: Int = 2
    ): Result<SsWebResult> {
        return try {
            val res = ApiClient.ssweb.capture(url, width, height, scale)
            val r = res.result
            if (res.status == true && r != null && !r.file_url.isNullOrBlank()) {
                Result.success(r)
            } else {
                Result.failure(Exception("gagal ambil screenshot"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
