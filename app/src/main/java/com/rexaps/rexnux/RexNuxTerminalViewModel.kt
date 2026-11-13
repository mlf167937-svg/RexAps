package com.rexaps.rexnux

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class RexNuxTerminalViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val runtime = RexNuxRuntime(application)
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var readerJob: Job? = null

    private var activeDistro: RexNuxDistro = RexNuxDistro.ALPINE

    private val _output = mutableStateOf("")
    val output: State<String> get() = _output

    private val _input = mutableStateOf("")
    val input: State<String> get() = _input

    private val _history = mutableListOf<String>()
    private var historyIndex = -1

    fun setInput(value: String) {
        _input.value = value
    }

    fun start(distro: RexNuxDistro) {
        if (process != null && activeDistro == distro) return
        stop()
        activeDistro = distro

        _output.value = """
            ==================================================
              RexNux Linux Environment (ARM64)
              Distro  : ${distro.title}
              Pkg Mgr : ${distro.packageManager}
              Shell   : ${distro.defaultShell}
              Home    : /files/home
            ==================================================

        """.trimIndent()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                append("Memulai proses PRoot container...\n")
                val p = runtime.startDistro(distro)
                process = p
                writer = OutputStreamWriter(p.outputStream)
                append("Lingkungan Linux aktif! Ketik perintah Anda di bawah.\n\n")

                readerJob = launch {
                    val reader = BufferedReader(InputStreamReader(p.inputStream))
                    val buffer = CharArray(4096)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count == -1) break
                        val text = String(buffer, 0, count)
                        append(text)
                    }
                    append("\n[Proses shell selesai / terminated]\n")
                }
            } catch (e: Exception) {
                append("\n[RexNux PRoot Error]\n")
                append("${e.message ?: "Kesalahan tidak diketahui"}\n")
            }
        }
    }

    fun execute() {
        val command = _input.value.trim()
        if (command.isEmpty()) return
        _history.add(command)
        historyIndex = _history.size
        _input.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = writer
                if (out == null) {
                    append("Shell belum aktif.\n")
                    return@launch
                }
                out.write(command + "\n")
                out.flush()
            } catch (e: Exception) {
                append("Error saat menulis perintah: ${e.message}\n")
            }
        }
    }

    fun sendControlKey(code: Char) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = writer ?: return@launch
                // Control character code: Ctrl+C is 0x03, Ctrl+D is 0x04, etc.
                val ctrlCode = (code.uppercaseChar().code - 'A'.code + 1)
                out.write(ctrlCode)
                out.flush()
            } catch (e: Exception) {
                append("Ctrl send error: ${e.message}\n")
            }
        }
    }

    fun sendSpecialKey(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = writer ?: return@launch
                when (key) {
                    "TAB" -> out.write("\t")
                    "ESC" -> out.write("\u001B")
                    "UP" -> out.write("\u001B[A")
                    "DOWN" -> out.write("\u001B[B")
                    "LEFT" -> out.write("\u001B[D")
                    "RIGHT" -> out.write("\u001B[C")
                    "ENTER" -> out.write("\n")
                }
                out.flush()
            } catch (e: Exception) {
                append("Key send error: ${e.message}\n")
            }
        }
    }

    fun historyPrev() {
        if (_history.isNotEmpty() && historyIndex > 0) {
            historyIndex--
            _input.value = _history[historyIndex]
        }
    }

    fun historyNext() {
        if (historyIndex < _history.size - 1) {
            historyIndex++
            _input.value = _history[historyIndex]
        } else {
            historyIndex = _history.size
            _input.value = ""
        }
    }

    fun clearScreen() {
        _output.value = ""
    }

    private fun append(value: String) {
        viewModelScope.launch(Dispatchers.Main) {
            // Keep last 50,000 chars to avoid memory bloat
            val current = _output.value
            val combined = current + value
            _output.value = if (combined.length > 50000) combined.takeLast(40000) else combined
        }
    }

    fun stop() {
        readerJob?.cancel()
        readerJob = null
        try {
            writer?.close()
        } catch (_: Exception) {}
        writer = null
        try {
            process?.destroy()
        } catch (_: Exception) {}
        process = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
