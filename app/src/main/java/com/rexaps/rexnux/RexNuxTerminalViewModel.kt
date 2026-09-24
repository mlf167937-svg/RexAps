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

    private val _output = mutableStateOf(
        "RexNux Alpine Linux\n" +
        "------------------------------\n"
    )
    val output: State<String> get() = _output

    private val _input = mutableStateOf("")
    val input: State<String> get() = _input

    fun setInput(value: String) {
        _input.value = value
    }

    fun start() {
        if (process != null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                append("Starting Alpine...\n")

                val p = runtime.startAlpine()
                process = p

                writer = OutputStreamWriter(p.outputStream)

                append("Alpine started.\n\n")

                readerJob = launch {
                    val reader = BufferedReader(
                        InputStreamReader(p.inputStream)
                    )

                    while (true) {
                        val line = reader.readLine() ?: break
                        append("$line\n")
                    }
                }
            } catch (e: Exception) {
                append("\nRexNux error:\n")
                append("${e.message ?: "Unknown error"}\n")
            }
        }
    }

    fun execute() {
        val command = _input.value.trim()
        if (command.isEmpty()) return

        _input.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = writer

                if (out == null) {
                    append("Shell belum berjalan.\n")
                    return@launch
                }

                out.write(command)
                out.write("\n")
                out.flush()
            } catch (e: Exception) {
                append("Command error: ${e.message}\n")
            }
        }
    }

    private fun append(value: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _output.value += value
        }
    }

    fun stop() {
        readerJob?.cancel()
        readerJob = null

        try {
            writer?.close()
        } catch (_: Exception) {
        }

        writer = null

        try {
            process?.destroy()
        } catch (_: Exception) {
        }

        process = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
