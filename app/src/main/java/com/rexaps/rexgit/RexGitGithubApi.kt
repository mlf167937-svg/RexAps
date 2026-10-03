package com.rexaps.rexgit

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RexGitGithubApi {

    fun getUser(token: String): String {
        return JSONObject(request("/user", token)).getString("login")
    }

    fun listRepositories(token: String): List<RexGitGithubRepository> {
        val result = mutableListOf<RexGitGithubRepository>()
        var page = 1

        while (page <= 20) {
            val array = JSONArray(
                request("/user/repos?per_page=100&page=$page&sort=updated", token)
            )

            if (array.length() == 0) break

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                result += RexGitGithubRepository(
                    name = item.getString("name"),
                    fullName = item.getString("full_name"),
                    cloneUrl = item.getString("clone_url"),
                    private = item.optBoolean("private", false),
                    defaultBranch = item.optString("default_branch", "main"),
                    description =
                        if (item.isNull("description")) null
                        else item.optString("description").ifBlank { null }
                )
            }

            if (array.length() < 100) break
            page++
        }

        // Paging while sorting by "updated" can return duplicates, which crash LazyColumn keys.
        return result.distinctBy { it.fullName }
    }

    private fun request(path: String, token: String): String {
        val connection = URL("https://api.github.com$path")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000

            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "RexGit")

            val code = connection.responseCode

            val stream =
                if (code in 200..299) connection.inputStream
                else connection.errorStream

            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val reason = try {
                    JSONObject(body).optString("message")
                } catch (_: Throwable) {
                    ""
                }

                throw IllegalStateException(
                    when (code) {
                        401 -> "Invalid or expired token (401)"
                        403 -> "Access denied or rate limited (403). $reason"
                        else -> "GitHub API $code. $reason"
                    }.trim()
                )
            }

            return body
        } finally {
            connection.disconnect()
        }
    }
}
