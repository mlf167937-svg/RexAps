package com.rexaps.rexwarp.ui.components

internal enum class DiagnosticStatus(val label: String) { PASS("PASS"), WARNING("WARNING"), FAIL("FAIL"), NOT_TESTED("NOT TESTED") }

internal data class DiagnosticCheck(val name: String, val status: DiagnosticStatus, val detail: String, val durationMs: Long? = null)

internal const val CHECK_HOST = "www.cloudflare.com"

internal const val TRACE_URL = "https://www.cloudflare.com/cdn-cgi/trace"

internal const val DNS_TIMEOUT_MS = 4_000L

internal const val SOCKET_TIMEOUT_MS = 4_000
