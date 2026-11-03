package com.rexaps.rexchat.v1

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RexChatRuntime {
    enum class Status { STOPPED, STARTING, READY, MISSING, ERROR }

    data class State(
        val status: Status = Status.STOPPED,
        val detail: String = "Runtime belum dijalankan",
        val qr: String? = null,
        val pairingCode: String? = null,
        val connected: Boolean = false
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionStates = ConcurrentHashMap<Int, MutableStateFlow<State>>()
    private val activeSessions = ConcurrentHashMap.newKeySet<Int>()
    private var process: Process? = null
    private var appContext: Context? = null
    private var writer: OutputStreamWriter? = null

    fun stateFor(sessionId: Int): StateFlow<State> =
        sessionStates.computeIfAbsent(sessionId) { MutableStateFlow(State()) }.asStateFlow()

    private fun setState(sessionId: Int, value: State) {
        sessionStates.computeIfAbsent(sessionId) { MutableStateFlow(State()) }.value = value
    }

    @Synchronized
    fun start(context: Context, sessionId: Int) {
        appContext = context.applicationContext
        activeSessions.add(sessionId)
        val current = process
        if (current?.isAlive == true) {
            setState(sessionId, State(Status.READY, "Runtime Node.js aktif"))
            send(JSONObject().put("type", "add-account").put("sessionId", sessionId))
            return
        }
        scope.launch {
            setState(sessionId, State(Status.STARTING, "Menyiapkan runtime RexChat…"))
            val home = File(context.applicationContext.filesDir, "rexchat").apply { mkdirs() }
            installAssets(context.applicationContext, home)
            val node = File(home, "runtime/node")
            val engine = File(home, "engine/index.js")
            if (activeSessions.isEmpty()) return@launch
            if (!node.isFile || !node.canExecute()) {
                setState(sessionId, State(
                    Status.MISSING,
                    "Binary Node.js 20+ belum disertakan untuk ABI perangkat ini."
                ))
                return@launch
            }
            if (!File(home, "node_modules/@whiskeysockets/baileys").isDirectory) {
                setState(sessionId, State(Status.MISSING, "Dependency Baileys belum dibundel pada runtime bersama."))
                return@launch
            }
            if (!engine.isFile) {
                setState(sessionId, State(Status.ERROR, "Engine RexChat tidak ditemukan."))
                return@launch
            }
            runCatching {
                val started = ProcessBuilder(node.absolutePath, engine.absolutePath)
                    .directory(home)
                    .redirectErrorStream(true)
                    .apply {
                        environment()["REXCHAT_HOME"] = home.absolutePath
                        environment()["REXCHAT_SESSION_ROOT"] = File(context.applicationContext.filesDir, "sessions").absolutePath
                    }
                    .start()
                synchronized(this@RexChatRuntime) {
                    if (activeSessions.isEmpty()) {
                        started.destroy()
                        return@runCatching
                    }
                    process = started
                    writer = OutputStreamWriter(started.outputStream, Charsets.UTF_8)
                    activeSessions.forEach { id ->
                        setState(id, State(Status.READY, "Runtime Node.js aktif"))
                        send(JSONObject().put("type", "add-account").put("sessionId", id))
                    }
                }
                BufferedReader(InputStreamReader(started.inputStream, Charsets.UTF_8)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        handleEvent(line)
                    }
                }
                synchronized(this@RexChatRuntime) {
                    if (process === started) {
                        process = null
                        writer = null
                        activeSessions.forEach { id -> setState(id, State(Status.STOPPED, "Runtime berhenti")) }
                    }
                }
            }.onFailure {
                synchronized(this@RexChatRuntime) {
                    process = null
                    writer = null
                    activeSessions.forEach { id -> setState(id, State(Status.ERROR, it.message ?: "Runtime gagal dijalankan")) }
                }
            }
        }
    }

    @Synchronized
    fun stopSession(sessionId: Int) {
        activeSessions.remove(sessionId)
        setState(sessionId, State(Status.STOPPED, "Session ditutup"))
        send(JSONObject().put("type", "remove-account").put("sessionId", sessionId))
        if (activeSessions.isEmpty()) {
            send(JSONObject().put("type", "shutdown"))
            val stopping = process
            process = null
            writer = null
            sessionStates.keys.forEach { id -> setState(id, State(Status.STOPPED, "Runtime dihentikan untuk menghemat RAM")) }
            if (stopping != null) {
                scope.launch {
                    if (!stopping.waitFor(1500, TimeUnit.MILLISECONDS)) stopping.destroyForcibly()
                }
            }
        }
    }

    @Synchronized
    fun startFromUi(sessionId: Int) {
        appContext?.let { start(it, sessionId) }
    }

    @Synchronized
    fun sendText(sessionId: Int, chatId: String, text: String) {
        send(JSONObject().put("type", "send-text").put("sessionId", sessionId).put("chatId", chatId).put("text", text))
    }

    @Synchronized
    fun requestPairingCode(sessionId: Int, phone: String) {
        send(JSONObject().put("type", "pairing-code").put("sessionId", sessionId).put("phone", phone))
    }

    @Synchronized
    private fun send(value: JSONObject) {
        runCatching {
            writer?.apply {
                write(value.toString())
                write("\n")
                flush()
            }
        }
    }

    private fun handleEvent(line: String) {
        val event = runCatching { JSONObject(line) }.getOrNull() ?: return
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
                else -> current
            }
            setState(id, updated)
        }
    }

    private fun installAssets(context: Context, home: File) {
        val mappings = mapOf(
            "rexchat/engine/index.js" to File(home, "engine/index.js"),
            "rexchat/plugins/menu.js" to File(home, "plugins/menu.js"),
            "rexchat/plugins/lib/richhtml.js" to File(home, "plugins/lib/richhtml.js"),
            "rexchat/package.json" to File(home, "package.json")
        )
        mappings.forEach { (asset, destination) ->
            if (!destination.exists()) {
                runCatching {
                    destination.parentFile?.mkdirs()
                    context.assets.open(asset).use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }
}
