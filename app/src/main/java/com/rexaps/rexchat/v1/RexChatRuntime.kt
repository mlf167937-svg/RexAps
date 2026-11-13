package com.rexaps.rexchat.v1

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object RexChatRuntime {
    private const val TAG = "RexChatRuntime"
    private const val HTTP_PORT = 43827
    private const val EXPECTED_NODE_VERSION = "v26.13.0"
    private const val ASSET_VERSION = "node-26.13.0-baileys-7.0.0-rc14-v1"
    private const val BASE_URL = "http://127.0.0.1:$HTTP_PORT"

    enum class Status { STOPPED, STARTING, READY, MISSING, ERROR }

    data class Message(
        val id: String,
        val chatId: String,
        val text: String,
        val fromMe: Boolean,
        val timestamp: Long
    )

    data class State(
        val status: Status = Status.STOPPED,
        val detail: String = "Runtime belum dijalankan",
        val qr: String? = null,
        val pairingCode: String? = null,
        val connected: Boolean = false,
        val messages: List<Message> = emptyList()
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionStates = ConcurrentHashMap<Int, kotlinx.coroutines.flow.MutableStateFlow<State>>()
    private val activeSessions = ConcurrentHashMap.newKeySet<Int>()
    private val authToken = UUID.randomUUID().toString().replace("-", "")

    @Volatile private var appContext: Context? = null
    @Volatile private var booting = false
    @Volatile private var nodeStartAttempted = false
    @Volatile private var nodeStarted = false
    @Volatile private var nativeRuntimeExitReason: String? = null
    private var eventPollJob: Job? = null

    fun stateFor(sessionId: Int): kotlinx.coroutines.flow.StateFlow<State> =
        sessionStates.computeIfAbsent(sessionId) {
            kotlinx.coroutines.flow.MutableStateFlow(State())
        }.asStateFlow()

    private fun setState(sessionId: Int, value: State) {
        sessionStates.computeIfAbsent(sessionId) {
            kotlinx.coroutines.flow.MutableStateFlow(State())
        }.value = value
    }

    @Synchronized
    fun start(context: Context, sessionId: Int) {
        val app = context.applicationContext
        appContext = app
        activeSessions.add(sessionId)

        if (nodeStarted) {
            setState(sessionId, State(Status.STARTING, "Menghubungkan ke runtime Node.js…"))
            postCommand(JSONObject().put("type", "add-account").put("sessionId", sessionId))
            return
        }

        if (booting) {
            setState(sessionId, State(Status.STARTING, "Menyiapkan runtime Node.js 26.13…"))
            return
        }

        if (nodeStartAttempted) {
            setState(
                sessionId,
                State(
                    Status.ERROR,
                    "Runtime Node.js gagal dimulai. Tutup RexAps sepenuhnya lalu buka kembali untuk mencoba lagi."
                )
            )
            return
        }

        booting = true
        nodeStartAttempted = true
        setState(sessionId, State(Status.STARTING, "Menyiapkan runtime Node.js 26.13…"))
        scope.launch { bootNode(app) }
    }

    private suspend fun bootNode(context: Context) {
        try {
            val abi = Build.SUPPORTED_ABIS.firstOrNull {
                it == "arm64-v8a" || it == "armeabi-v7a"
            }
            if (abi == null) {
                throw IllegalStateException(
                    "ABI perangkat tidak didukung. RexChat memerlukan ARM64-v8a atau ARM32-v7a."
                )
            }

            val home = File(context.filesDir, "rexchat").apply { mkdirs() }
            installAssets(context, home)

            val engine = File(home, "engine/index.js")
            val baileys = File(home, "node_modules/@whiskeysockets/baileys/package.json")
            if (!engine.isFile) {
                throw IllegalStateException("Engine RexChat tidak ditemukan di assets aplikasi.")
            }
            if (!baileys.isFile) {
                throw IllegalStateException(
                    "Dependency Baileys belum terpasang di APK. Build ulang dengan step npm install RexChat."
                )
            }

            val sessionRoot = File(context.filesDir, "sessions").apply { mkdirs() }
            val arguments = arrayOf(
                "node",
                engine.absolutePath,
                "--rexchat-port=$HTTP_PORT",
                "--rexchat-token=$authToken",
                "--rexchat-session-root=${sessionRoot.absolutePath}"
            )

            // node::Start() blocks for the lifetime of the Node event loop. Keep it off
            // the UI thread and never attempt to start a second Node instance in-process.
            nativeRuntimeExitReason = null
            scope.launch {
                runCatching { RexNodeBridge.startNodeWithArguments(arguments) }
                    .onSuccess { code ->
                        nativeRuntimeExitReason = "Node.js berhenti dengan exit code $code"
                        Log.i(TAG, "Embedded Node.js stopped with exit code $code")
                        markRuntimeStopped("Runtime Node.js berhenti (exit $code).")
                    }
                    .onFailure { error ->
                        val message = error.message ?: error.javaClass.simpleName
                        nativeRuntimeExitReason = message
                        Log.e(TAG, "Unable to start embedded Node.js", error)
                        markRuntimeError("Gagal memuat runtime Node.js: $message")
                    }
            }

            var ready = false
            for (attempt in 0 until 100) {
                nativeRuntimeExitReason?.let { reason ->
                    throw IllegalStateException("Embedded Node.js gagal dimulai: $reason")
                }
                if (healthCheck()) {
                    ready = true
                    break
                }
                delay(300)
            }
            if (!ready) {
                throw IllegalStateException(
                    "libnode.so berhasil dipanggil, tetapi server RexChat tidak merespons. Periksa ABI dan log Node.js."
                )
            }

            synchronized(this) {
                nodeStarted = true
                booting = false
                activeSessions.forEach { id ->
                    setState(id, State(Status.READY, "Runtime Node.js 26.13 aktif"))
                }
            }

            activeSessions.forEach { id ->
                postCommand(JSONObject().put("type", "add-account").put("sessionId", id))
            }
            startEventPolling()
        } catch (error: Throwable) {
            Log.e(TAG, "RexChat bootstrap failed", error)
            synchronized(this) {
                booting = false
                nodeStarted = false
                activeSessions.forEach { id ->
                    setState(id, State(Status.ERROR, error.message ?: "Runtime RexChat gagal dimulai"))
                }
            }
        }
    }

    @Synchronized
    fun stopSession(sessionId: Int) {
        activeSessions.remove(sessionId)
        setState(sessionId, State(Status.STOPPED, "Session ditutup"))
        if (nodeStarted) {
            postCommand(JSONObject().put("type", "remove-account").put("sessionId", sessionId))
        }
        // Keep the embedded Node event loop alive. Node/V8 cannot safely be restarted
        // in the same Android process after node::Start() has returned.
    }

    @Synchronized
    fun startFromUi(sessionId: Int) {
        appContext?.let { start(it, sessionId) }
            ?: setState(sessionId, State(Status.ERROR, "Konteks aplikasi belum tersedia. Buka ulang RexChat."))
    }

    @Synchronized
    fun sendText(sessionId: Int, chatId: String, text: String) {
        postCommand(
            JSONObject()
                .put("type", "send-text")
                .put("sessionId", sessionId)
                .put("chatId", chatId)
                .put("text", text)
        )
    }

    @Synchronized
    fun requestPairingCode(sessionId: Int, phone: String) {
        if (!nodeStarted) {
            setState(sessionId, State(Status.STARTING, "Runtime Node.js belum siap. Muat ulang setelah runtime aktif."))
            return
        }
        postCommand(
            JSONObject()
                .put("type", "pairing-code")
                .put("sessionId", sessionId)
                .put("phone", phone.filter(Char::isDigit))
        )
    }

    private fun postCommand(command: JSONObject) {
        scope.launch {
            runCatching { request("POST", "/command", command.toString()) }
                .onFailure { error ->
                    val id = command.optInt("sessionId", -1)
                    if (id > 0) {
                        val current = stateFor(id).value
                        setState(id, current.copy(status = Status.ERROR, detail = error.message ?: "Gagal mengirim perintah ke Node.js"))
                    }
                    Log.e(TAG, "RexChat command failed", error)
                }
        }
    }

    private fun startEventPolling() {
        if (eventPollJob?.isActive == true) return
        eventPollJob = scope.launch {
            while (isActive) {
                try {
                    val result = request("GET", "/events")
                    val events = result.optJSONArray("events") ?: JSONArray()
                    for (index in 0 until events.length()) {
                        handleEvent(events.optJSONObject(index) ?: continue)
                    }
                } catch (error: Throwable) {
                    if (nodeStarted && activeSessions.isNotEmpty()) {
                        Log.w(TAG, "RexChat event polling failed: ${error.message}")
                    }
                }
                delay(450)
            }
        }
    }

    private fun healthCheck(): Boolean {
        val health = runCatching { request("GET", "/health") }.getOrNull() ?: return false
        val actualVersion = health.optString("version")
        if (actualVersion.isNotBlank() && !actualVersion.startsWith("v26")) {
            Log.w(TAG, "Node.js version mismatch: $actualVersion (expected $EXPECTED_NODE_VERSION)")
        }
        return health.optBoolean("ok", false)
    }

    private fun request(method: String, path: String, body: String? = null): JSONObject {
        val connection = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 1800
            readTimeout = 30000
            setRequestProperty("X-RexChat-Token", authToken)
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("RexChat HTTP $status: ${response.take(240)}")
            }
            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun handleEvent(event: JSONObject) {
        val sessionId = event.optInt("sessionId", -1)
        val targets = if (sessionId > 0) listOf(sessionId) else activeSessions.toList()
        targets.forEach { id ->
            val current = stateFor(id).value
            val updated = when (event.optString("event")) {
                "qr" -> current.copy(
                    status = Status.READY,
                    detail = "Pindai QR dari WhatsApp",
                    qr = event.optString("value").takeIf { it.isNotBlank() },
                    pairingCode = null
                )
                "pairing-code" -> current.copy(
                    status = Status.READY,
                    detail = "Kode pairing siap",
                    pairingCode = event.optString("value").takeIf { it.isNotBlank() }
                )
                "connected" -> {
                    appContext?.let { com.rexaps.rexchat.SessionManager.markLinked(it, id, true) }
                    current.copy(
                        status = Status.READY,
                        detail = "WhatsApp terhubung",
                        connected = true,
                        qr = null,
                        pairingCode = null
                    )
                }
                "disconnected" -> current.copy(
                    detail = event.optString("reason", "Koneksi terputus"),
                    connected = false
                )
                "error" -> current.copy(
                    status = Status.ERROR,
                    detail = event.optString("message", "Terjadi kesalahan runtime")
                )
                "message" -> {
                    val idValue = event.optString("id")
                    val message = Message(
                        id = idValue,
                        chatId = event.optString("chatId"),
                        text = event.optString("text"),
                        fromMe = event.optBoolean("fromMe"),
                        timestamp = event.optLong("timestamp", 0L)
                    )
                    if (message.chatId.isBlank() || message.text.isBlank()) current
                    else {
                        val withoutDuplicate = if (idValue.isBlank()) current.messages
                            else current.messages.filterNot { it.id == idValue }
                        current.copy(messages = (withoutDuplicate + message).takeLast(100))
                    }
                }
                else -> current
            }
            setState(id, updated)
        }
    }

    private suspend fun installAssets(context: Context, home: File) = withContext(Dispatchers.IO) {
        val versionFile = File(home, ".runtime-version")
        val installedVersion = versionFile.takeIf { it.isFile }?.readText()?.trim()
        if (installedVersion != ASSET_VERSION) {
            listOf("engine", "plugins", "node_modules", "package.json").forEach { relative ->
                File(home, relative).deleteRecursively()
            }
        }

        copyAssetTree(context, "rexchat", home)
        versionFile.writeText(ASSET_VERSION)
    }

    private fun copyAssetTree(context: Context, assetPath: String, destination: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isNotEmpty()) {
            destination.mkdirs()
            children.forEach { child ->
                copyAssetTree(context, "$assetPath/$child", File(destination, child))
            }
            return
        }

        runCatching {
            destination.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to copy asset $assetPath: ${error.message}")
        }
    }

    @Synchronized
    private fun markRuntimeStopped(message: String) {
        nodeStarted = false
        booting = false
        activeSessions.forEach { id -> setState(id, State(Status.STOPPED, message)) }
    }

    @Synchronized
    private fun markRuntimeError(message: String) {
        nodeStarted = false
        booting = false
        activeSessions.forEach { id -> setState(id, State(Status.ERROR, message)) }
    }
}
