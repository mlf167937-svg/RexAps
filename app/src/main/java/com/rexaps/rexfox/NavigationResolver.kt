package com.rexaps.rexfox

import android.net.Uri

sealed interface NavigationTarget {
    data class Web(val url: String) : NavigationTarget
    data class Search(val url: String) : NavigationTarget
    data class External(val uri: Uri) : NavigationTarget
    data object Empty : NavigationTarget
}

object NavigationResolver {
    private val webSchemes = setOf("http", "https", "about", "file", "data", "blob")
    private val externalSchemes = setOf("mailto", "tel", "sms", "geo", "intent")

    fun resolve(input: String, engine: SearchEngine): NavigationTarget {
        val value = input.trim()
        if (value.isEmpty()) return NavigationTarget.Empty

        val uri = runCatching { Uri.parse(value) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()

        if (scheme in webSchemes) return NavigationTarget.Web(value)
        if (scheme in externalSchemes && uri != null) return NavigationTarget.External(uri)

        val domain = !value.contains(" ") &&
            value.matches(Regex("""^[A-Za-z0-9.-]+\.[A-Za-z]{2,}([/:?#].*)?$"""))

        if (domain) return NavigationTarget.Web("https://$value")

        val search = engine.searchUrl(value)
        return if (search != null) NavigationTarget.Search(search) else NavigationTarget.Empty
    }
}
