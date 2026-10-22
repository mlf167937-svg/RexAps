package com.rexaps.rexgit

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RexGitGithubApi {

    fun getUser(token: String): String = JSONObject(request("/user", token)).getString("login")

    fun listRepositories(token: String): List<RexGitGithubRepository> {
        val result = mutableListOf<RexGitGithubRepository>()
        var page = 1
        while (page <= 20) {
            val array = JSONArray(request("/user/repos?per_page=100&page=$page&sort=updated", token))
            if (array.length() == 0) break
            for (i in 0 until array.length()) result += parseRepository(array.getJSONObject(i))
            if (array.length() < 100) break
            page++
        }
        return result.distinctBy { it.fullName }
    }

    /** Searches public and token-visible repositories by keyword or owner/repo. */
    fun searchRepositories(token: String, query: String): List<RexGitGithubRepository> {
        val clean = query.trim()
        if (clean.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(clean, Charsets.UTF_8.name())
        val array = JSONObject(request("/search/repositories?q=$encoded&per_page=50&sort=updated", token))
            .optJSONArray("items") ?: JSONArray()
        return (0 until array.length()).map { parseRepository(array.getJSONObject(it)) }
            .distinctBy { it.fullName }
    }

    /** Reads full repository metadata for the repo header and README preview. */
    fun getRepository(token: String?, owner: String, repository: String): RexGitGithubRepository {
        val slug = "/repos/${pathPart(owner)}/${pathPart(repository)}"
        return parseRepository(JSONObject(request(slug, token.orEmpty())))
    }

    /** Lists tags separately because a repository can have tags without published Releases. */
    fun listTags(token: String?, owner: String, repository: String): List<String> {
        val slug = "/repos/${pathPart(owner)}/${pathPart(repository)}/tags?per_page=100"
        val json = JSONArray(request(slug, token.orEmpty()))
        return (0 until json.length()).mapNotNull { index ->
            json.getJSONObject(index).optString("name").takeIf { it.isNotBlank() && it != "null" }
        }
    }

    fun loadActions(token: String?, owner: String, repository: String): RexGitActionsSnapshot {
        val slug = "/repos/${pathPart(owner)}/${pathPart(repository)}"
        val workflowsJson = JSONArray(request("$slug/actions/workflows?per_page=100", token.orEmpty()))
        val workflows = (0 until workflowsJson.length()).map { i ->
            val item = workflowsJson.getJSONObject(i)
            RexGitWorkflow(
                id = item.optLong("id"),
                name = item.optString("name", "Workflow"),
                path = item.optString("path"),
                state = item.optString("state", "unknown"),
                htmlUrl = item.optString("html_url")
            )
        }

        val runsJson = JSONObject(request("$slug/actions/runs?per_page=20", token.orEmpty()))
            .optJSONArray("workflow_runs") ?: JSONArray()
        val runs = (0 until runsJson.length()).map { i ->
            val item = runsJson.getJSONObject(i)
            RexGitWorkflowRun(
                id = item.optLong("id"),
                name = item.optString("name", "Workflow"),
                title = item.optString("display_title", item.optString("name", "Workflow run")),
                event = item.optString("event", "unknown"),
                status = item.optString("status", "unknown"),
                conclusion = item.optString("conclusion").takeIf { it.isNotBlank() && it != "null" },
                createdAt = item.optString("created_at"),
                updatedAt = item.optString("updated_at"),
                htmlUrl = item.optString("html_url"),
                runNumber = item.optInt("run_number"),
                branch = item.optString("head_branch"),
                headSha = item.optString("head_sha"),
                commitMessage = item.optJSONObject("head_commit")?.optString("message").orEmpty(),
                actor = item.optJSONObject("actor")?.optString("login").orEmpty()
            )
        }

        // GitHub exposes artifacts per run. Limit the extra requests to the 8 newest runs.
        val artifacts = mutableListOf<RexGitArtifact>()
        runs.take(8).forEach { run ->
            try {
                val json = JSONObject(request("$slug/actions/runs/${run.id}/artifacts?per_page=100", token.orEmpty()))
                    .optJSONArray("artifacts") ?: JSONArray()
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    artifacts += RexGitArtifact(
                        id = item.optLong("id"),
                        name = item.optString("name", "artifact.zip"),
                        sizeInBytes = item.optLong("size_in_bytes"),
                        expired = item.optBoolean("expired", false),
                        archiveDownloadUrl = item.optString("archive_download_url"),
                        workflowRunId = run.id,
                        workflowName = run.title
                    )
                }
            } catch (_: Throwable) {
                // Missing workflow permission or a deleted run should not hide other runs.
            }
        }
        return RexGitActionsSnapshot(workflows, runs, artifacts)
    }

    fun listReleases(token: String?, owner: String, repository: String): List<RexGitRelease> {
        val slug = "/repos/${pathPart(owner)}/${pathPart(repository)}/releases?per_page=50"
        val json = JSONArray(request(slug, token.orEmpty()))
        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val assetsJson = item.optJSONArray("assets") ?: JSONArray()
            val assets = (0 until assetsJson.length()).map { i ->
                val asset = assetsJson.getJSONObject(i)
                RexGitReleaseAsset(
                    id = asset.optLong("id"),
                    name = asset.optString("name", "asset"),
                    sizeInBytes = asset.optLong("size"),
                    // This is the API asset URL. It can be downloaded with a token without leaving RexGit.
                    downloadUrl = asset.optString("url"),
                    contentType = asset.optString("content_type", "application/octet-stream")
                )
            }
            RexGitRelease(
                id = item.optLong("id"),
                tagName = item.optString("tag_name"),
                name = item.optString("name").ifBlank { item.optString("tag_name") },
                body = item.optString("body"),
                publishedAt = item.optString("published_at"),
                htmlUrl = item.optString("html_url"),
                draft = item.optBoolean("draft", false),
                prerelease = item.optBoolean("prerelease", false),
                assets = assets
            )
        }
    }

    /** Downloads an artifact or release asset into a local file, following GitHub's signed redirects safely. */
    fun download(
        url: String,
        token: String?,
        destination: File,
        accept: String = "application/vnd.github+json",
        onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
    ) {
        destination.parentFile?.mkdirs()
        var current = URL(url)
        var redirects = 0
        while (true) {
            val connection = current.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 60000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "RexGit")
                connection.setRequestProperty("Accept", accept)
                if (current.host.equals("api.github.com", true) && !token.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("GitHub returned a redirect without a download URL")
                    if (++redirects > 6) throw IllegalStateException("Too many redirects while downloading")
                    current = URL(current, location)
                    continue
                }
                if (code !in 200..299) {
                    val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val reason = try { JSONObject(body).optString("message") } catch (_: Throwable) { body.take(180) }
                    throw IllegalStateException("Download failed ($code): $reason")
                }
                val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
                var receivedBytes = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                connection.inputStream.use { input ->
                    destination.outputStream().buffered().use { output ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            receivedBytes += count
                            onProgress(receivedBytes, totalBytes)
                        }
                    }
                }
                onProgress(receivedBytes, totalBytes ?: receivedBytes)
                return
            } catch (t: Throwable) {
                // Do not leave a half-downloaded ZIP/asset behind after a network or storage failure.
                destination.delete()
                throw t
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseRepository(item: JSONObject) = RexGitGithubRepository(
        name = item.optString("name"),
        fullName = item.optString("full_name"),
        cloneUrl = item.optString("clone_url"),
        private = item.optBoolean("private", false),
        defaultBranch = item.optString("default_branch", "main"),
        description = item.optString("description").takeIf { it.isNotBlank() && it != "null" },
        stars = item.optInt("stargazers_count"),
        htmlUrl = item.optString("html_url").takeIf { it.isNotBlank() },
        forks = item.optInt("forks_count"),
        language = item.optString("language").takeIf { it.isNotBlank() && it != "null" },
        licenseName = item.optJSONObject("license")?.optString("name")?.takeIf { it.isNotBlank() && it != "null" },
        updatedAt = item.optString("updated_at").takeIf { it.isNotBlank() && it != "null" },
        ownerLogin = item.optJSONObject("owner")?.optString("login")?.takeIf { it.isNotBlank() && it != "null" },
        avatarUrl = item.optJSONObject("owner")?.optString("avatar_url")?.takeIf { it.isNotBlank() && it != "null" }
    )

    private fun pathPart(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
        .replace("+", "%20")

    private fun request(path: String, token: String): String {
        val connection = URL("https://api.github.com$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "RexGit")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val reason = try { JSONObject(body).optString("message") } catch (_: Throwable) { "" }
                throw IllegalStateException(
                    when (code) {
                        401 -> "Invalid or expired token (401)"
                        403 -> "Access denied or rate limited (403). For private repositories, check that the selected token can access this repository and has Actions: read permission. $reason"
                        404 -> "Repository, workflow, or release not found (404). GitHub may also return 404 for a private repository the selected token cannot access; verify the owner/repository and token repository access."
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

data class RexGitActionsSnapshot(
    val workflows: List<RexGitWorkflow>,
    val runs: List<RexGitWorkflowRun>,
    val artifacts: List<RexGitArtifact>
)
