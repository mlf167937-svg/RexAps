package com.rexaps.rextools.tiktok

import com.rexaps.rextools.TiktokDownloadResult
import com.rexaps.rextools.TiktokStalkResult
import com.rexaps.rextools.utils.ApiClient

object TiktokApi {

    suspend fun stalk(username: String): Result<TiktokStalkResult> {
        val clean = username.trim().trimStart('@')
        if (clean.isBlank()) return Result.failure(Exception("username kosong"))
        return try {
            val res = ApiClient.tiktok.stalkTiktok(clean)
            val r = res.result
            if (res.status == true && r != null) {
                Result.success(r)
            } else {
                Result.failure(Exception("user tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(url: String): Result<TiktokDownloadResult> {
        val clean = url.trim()
        if (clean.isBlank()) return Result.failure(Exception("url kosong"))
        return try {
            val res = ApiClient.tiktok.downloadTiktok(clean)
            val r = res.result
            if (res.status == true && r != null && !r.data.isNullOrEmpty()) {
                Result.success(r)
            } else {
                Result.failure(Exception("gagal mengambil media"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
