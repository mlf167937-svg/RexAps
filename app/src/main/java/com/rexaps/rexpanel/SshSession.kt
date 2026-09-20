package com.rexaps.rexpanel

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

data class SshTarget(val host: String, val port: Int, val user: String)

/** Membaca: ssh -p 8022 root@192.168.0.101 */
fun parseSshCommand(input: String): SshTarget? {
    val tokens = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    var port = 22
    var dest: String? = null
    var i = 0
    while (i < tokens.size) {
        val t = tokens[i]
        when {
            t == "ssh" -> {}
            t == "-p" -> { port = tokens.getOrNull(i + 1)?.toIntOrNull() ?: return null; i++ }
            t.startsWith("-p") && t.length > 2 -> port = t.drop(2).toIntOrNull() ?: return null
            t.startsWith("-") -> {}
            else -> dest = t
        }
        i++
    }
    val d = dest ?: return null
    val user = if ('@' in d) d.substringBefore('@') else "root"
    val host = d.substringAfter('@')
    if (host.isBlank()) return null
    return SshTarget(host, port, user)
}

class SshSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: Session? = null
    private var shell: ChannelShell? = null
    private val outQueue = Channel<ByteArray>(Channel.UNLIMITED)

    suspend fun connect(
        target: SshTarget,
        password: String,
        cols: Int,
        rows: Int,
        onText: (String) -> Unit,
        onClosed: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val s = JSch().getSession(target.user, target.host, target.port)
            s.setPassword(password)
            // Versi awal: host key tidak diverifikasi. Untuk produksi, simpan dan cek fingerprint.
            s.setConfig("StrictHostKeyChecking", "no")
            s.setServerAliveInterval(15_000)
            s.connect(10_000)

            val ch = s.openChannel("shell") as ChannelShell
            ch.setPtyType("xterm-256color")
            ch.setPtySize(cols, rows, 0, 0)
            val input = ch.inputStream
            val output = ch.outputStream
            ch.connect(10_000)
            session = s
            shell = ch

            scope.launch {
                val reader = InputStreamReader(input, Charsets.UTF_8)
                val buf = CharArray(4096)
                try {
                    while (true) {
                        val n = reader.read(buf)
                        if (n < 0) break
                        onText(String(buf, 0, n))
                    }
                } catch (_: Exception) {
                }
                onClosed()
            }
            scope.launch {
                try {
                    for (bytes in outQueue) {
                        output.write(bytes)
                        output.flush()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun send(bytes: ByteArray) {
        outQueue.trySend(bytes)
    }

    fun resize(cols: Int, rows: Int) {
        scope.launch { runCatching { shell?.setPtySize(cols, rows, 0, 0) } }
    }

    suspend fun exec(cmd: String): String = withContext(Dispatchers.IO) {
        val s = session ?: error("Tidak terhubung")
        val ch = s.openChannel("exec") as ChannelExec
        ch.setCommand(cmd)
        val input = ch.inputStream
        ch.connect(5_000)
        try {
            input.readBytes().toString(Charsets.UTF_8)
        } finally {
            ch.disconnect()
        }
    }

    fun close() {
        runCatching { shell?.disconnect() }
        runCatching { session?.disconnect() }
        scope.cancel()
    }
}
