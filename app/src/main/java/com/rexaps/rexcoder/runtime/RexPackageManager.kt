package com.rexaps.rexcoder.runtime

import android.content.Context
import android.os.Build
import com.rexaps.rexcoder.storage.WorkspaceManager
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Tiny RexCoder package manager for language runtimes.
 * Registry packages are ZIP archives with an Android ABI-specific manifest entry.
 */
object RexPackageManager {
    private lateinit var context: Context
    private const val CONFIG_NAME = "config.json"
    private const val REGISTRY_NAME = "registry.json"
    private const val DEFAULT_TIMEOUT = 30_000

    data class PackageInfo(val id: String, val version: String, val abi: String, val url: String, val sha256: String, val archiveRoot: String = "")

    fun init(context: Context) {
        this.context = context.applicationContext
        ensureLayout()
    }

    fun ensureLayout() {
        val root = File(WorkspaceManager.root, ".rex")
        require(root.exists() || root.mkdirs()) { "Tidak bisa membuat folder .rex" }
        val packages = File(root, "packages")
        require(packages.exists() || packages.mkdirs()) { "Tidak bisa membuat folder .rex/packages" }
        val config = File(root, CONFIG_NAME)
        if (!config.exists()) {
            config.parentFile?.mkdirs()
            config.writeText(DEFAULT_CONFIG)
        }
        val registry = File(root, REGISTRY_NAME)
        if (!registry.exists()) {
            registry.parentFile?.mkdirs()
            registry.writeText(DEFAULT_REGISTRY)
        }
    }

    fun install(id: String, requestedVersion: String? = null): Result<String> = runCatching {
        val normalized = id.lowercase()
        require(normalized == "python" || normalized == "node") { "Unsupported Rex package: $id. Supported: python, node" }
        val registry = loadRegistry()
        val pkg = selectPackage(registry, normalized, requestedVersion)
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: error("Android ABI tidak terdeteksi")
        val spec = pkg.firstOrNull { it.abi == abi } ?: error("$normalized tidak tersedia untuk ABI $abi")
        val target = File(WorkspaceManager.root, ".language/$normalized/${spec.version}")
        val marker = File(target, ".installed.json")
        if (marker.isFile && marker.readText().contains(spec.sha256, ignoreCase = true)) {
            activate(normalized, spec.version)
            return@runCatching "$normalized ${spec.version} sudah terpasang ($abi)"
        }
        target.mkdirs()
        val archive = File(target.parentFile, "${normalized}-${spec.version}-${abi}.download")
        download(spec.url, archive)
        verifySha256(archive, spec.sha256)
        extractZip(archive, target)
        archive.delete()
        marker.writeText(JSONObject().apply {
            put("id", normalized); put("version", spec.version); put("abi", abi); put("sha256", spec.sha256)
        }.toString(2))
        activate(normalized, spec.version)
        "$normalized ${spec.version} installed for $abi"
    }

    fun listInstalled(): List<String> {
        ensureLayout()
        return listOf("python", "node").flatMap { id ->
            File(WorkspaceManager.root, ".language/$id").listFiles()?.filter { it.isDirectory && File(it, ".installed.json").isFile }?.map { "$id ${it.name}" } ?: emptyList()
        }
    }

    fun registryUrl(): String = readConfig().optString("registryUrl", "").trim()

    fun setRegistryUrl(url: String) {
        require(url.startsWith("https://") || url.startsWith("http://")) { "Registry URL harus http(s)" }
        val config = readConfig().apply { put("registryUrl", url) }
        File(WorkspaceManager.root, ".rex/$CONFIG_NAME").writeText(config.toString(2))
    }

    private fun selectPackage(registry: JSONObject, id: String, version: String?): List<PackageInfo> {
        val versions = registry.getJSONObject("packages").getJSONObject(id).getJSONObject("versions")
        val selected = version ?: versions.keys().asSequence().toList().sortedDescending().firstOrNull() ?: error("Tidak ada versi untuk $id")
        val versionObj = versions.optJSONObject(selected) ?: error("Versi $id@$selected tidak ada di registry")
        val android = versionObj.optJSONObject("android") ?: error("Registry $id@$selected tidak punya target Android")
        val result = mutableListOf<PackageInfo>()
        val keys = android.keys()
        while (keys.hasNext()) {
            val abi = keys.next(); val item = android.getJSONObject(abi)
            result += PackageInfo(id, selected, abi, item.getString("url"), item.getString("sha256"), item.optString("archiveRoot"))
        }
        return result
    }

    private fun loadRegistry(): JSONObject {
        ensureLayout()
        val url = registryUrl()
        if (url.isBlank()) return JSONObject(File(WorkspaceManager.root, ".rex/$REGISTRY_NAME").readText())
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = DEFAULT_TIMEOUT; connection.readTimeout = DEFAULT_TIMEOUT; connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }.also { connection.disconnect() }
    }

    private fun download(url: String, out: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = DEFAULT_TIMEOUT; connection.readTimeout = 120_000; connection.requestMethod = "GET"
        require(connection.responseCode in 200..299) { "Download gagal: HTTP ${connection.responseCode}" }
        connection.inputStream.use { input -> out.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
        connection.disconnect()
    }

    private fun verifySha256(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buf = ByteArray(64 * 1024); while (true) { val n = input.read(buf); if (n < 0) break; digest.update(buf, 0, n) } }
        val bytes = digest.digest()
        val hex = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xff
            if (v < 16) hex.append('0')
            hex.append(v.toString(16))
        }
        val actual = hex.toString()
        require(actual.equals(expected, ignoreCase = true)) { "SHA-256 mismatch untuk ${file.name}" }
    }

    private fun extractZip(zip: File, target: File) {
        val base = target.canonicalFile
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val out = File(target, entry.name).canonicalFile
                require(out.path == base.path || out.path.startsWith(base.path + File.separator)) { "Unsafe archive path: ${entry.name}" }
                if (entry.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); out.outputStream().use { input.copyTo(it, 64 * 1024) }; if (entry.name.contains("/bin/") || entry.name.startsWith("bin/")) out.setExecutable(true, false) }
            }
        }
    }

    private fun activate(id: String, version: String) {
        val active = JSONObject().put("version", version)
        File(WorkspaceManager.root, ".language/$id").apply { mkdirs() }.resolve("active.json").writeText(active.toString(2))
    }

    private fun readConfig(): JSONObject {
        ensureLayout(); return JSONObject(File(WorkspaceManager.root, ".rex/$CONFIG_NAME").readText())
    }

    private const val DEFAULT_CONFIG = """{\n  \"registryUrl\": \"\"\n}"""

    private const val DEFAULT_REGISTRY = """{\n  \"version\": 1,\n  \"packages\": {\n    \"python\": {\"versions\": {}},\n    \"node\": {\"versions\": {}}\n  }\n}"""
}
