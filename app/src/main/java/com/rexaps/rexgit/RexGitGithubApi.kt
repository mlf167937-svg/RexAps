package com.rexaps.rexgit

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RexGitGithubApi {

    fun getUser(
        token: String
    ): String {

        val json = request(
            "/user",
            token
        )

        return JSONObject(json)
            .getString("login")
    }

    fun listRepositories(
        token: String
    ): List<RexGitGithubRepository> {

        val result =
            mutableListOf<RexGitGithubRepository>()

        var page = 1

        while (true) {

            val json = request(
                "/user/repos?per_page=100&page=$page&sort=updated",
                token
            )

            val array =
                JSONArray(json)

            if (array.length() == 0) {
                break
            }

            for (i in 0 until array.length()) {

                val item =
                    array.getJSONObject(i)

                result += RexGitGithubRepository(
                    name = item.getString("name"),
                    fullName = item.getString("full_name"),
                    cloneUrl = item.getString("clone_url"),
                    private = item.getBoolean("private"),
                    defaultBranch =
                        item.optString(
                            "default_branch",
                            "main"
                        )
                )
            }

            if (array.length() < 100) {
                break
            }

            page++
        }

        return result
    }

    private fun request(
        path: String,
        token: String
    ): String {

        val connection =
            URL("https://api.github.com$path")
                .openConnection()
                    as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        connection.setRequestProperty(
            "Authorization",
            "Bearer $token"
        )

        connection.setRequestProperty(
            "Accept",
            "application/vnd.github+json"
        )

        connection.setRequestProperty(
            "X-GitHub-Api-Version",
            "2022-11-28"
        )

        val code =
            connection.responseCode

        val stream =
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val body =
            stream
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""

        connection.disconnect()

        if (code !in 200..299) {
            throw IllegalStateException(
                "GitHub API $code: $body"
            )
        }

        return body
    }
}
