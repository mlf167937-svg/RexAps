package com.rexaps.rextools.search

import com.rexaps.rextools.PinterestResponse
import com.rexaps.rextools.utils.ApiClient

object PinterestApi {
    suspend fun search(query: String): Result<List<String>> {
        return try {
            val res: PinterestResponse = ApiClient.pinterest.searchPinterest(query)
            if (res.status && !res.result.isNullOrEmpty()) {
                Result.success(res.result)
            } else {
                Result.failure(Exception("hasil kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
