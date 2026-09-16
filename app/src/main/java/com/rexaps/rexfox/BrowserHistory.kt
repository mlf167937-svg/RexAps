package com.rexaps.rexfox

class BrowserHistory {

    private val history = mutableListOf<String>()

    fun add(url: String) {
        if (url.isEmpty()) return

        if (history.lastOrNull() != url) {
            history.add(url)
        }
    }

    fun getAll(): List<String> {
        return history.toList()
    }

    fun clear() {
        history.clear()
    }
}