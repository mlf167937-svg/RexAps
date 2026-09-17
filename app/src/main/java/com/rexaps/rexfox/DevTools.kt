package com.rexaps.rexfox

import android.content.Context
import android.webkit.WebView
import java.io.File

object DevTools {

    /**
     * Injects JavaScript into the WebView to extract the full HTML source,
     * then returns it via the callback.
     */
    fun extractPageSource(
        webView: WebView,
        onResult: (html: String) -> Unit
    ) {
        // This JS grabs the full live DOM (not just original source)
        val js = """
            (function() {
                var html = '<!DOCTYPE html>\n' + document.documentElement.outerHTML;
                return html;
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            // evaluateJavascript wraps the string in JSON quotes, unescape it
            val cleaned = result
                ?.removePrefix("\"")
                ?.removeSuffix("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t")
                ?.replace("\\\"", "\"")
                ?.replace("\\/", "/")
                ?: "<html><body>Could not extract source.</body></html>"
            onResult(cleaned)
        }
    }

    /**
     * Saves the HTML string to a file in the app's cache dir
     * and returns the File for sharing/downloading.
     */
    fun saveHtmlToFile(context: Context, html: String, filename: String = "page_source.html"): File {
        val file = File(context.cacheDir, filename)
        file.writeText(html, Charsets.UTF_8)
        return file
    }

    /**
     * Injects JS to collect basic page diagnostics:
     * - title, URL, number of scripts, stylesheets, images, links
     */
    fun extractPageInfo(
        webView: WebView,
        onResult: (info: PageInfo) -> Unit
    ) {
        val js = """
            (function() {
                return JSON.stringify({
                    title: document.title,
                    url: window.location.href,
                    scripts: document.getElementsByTagName('script').length,
                    stylesheets: document.getElementsByTagName('link').length,
                    images: document.getElementsByTagName('img').length,
                    links: document.getElementsByTagName('a').length,
                    metaDesc: (document.querySelector('meta[name="description"]') || {content:''}).content,
                    charset: document.characterSet
                });
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            try {
                val json = org.json.JSONObject(
                    result?.removePrefix("\"")
                        ?.removeSuffix("\"")
                        ?.replace("\\\"", "\"")
                        ?: "{}"
                )
                onResult(
                    PageInfo(
                        title = json.optString("title", "—"),
                        url = json.optString("url", "—"),
                        scripts = json.optInt("scripts", 0),
                        stylesheets = json.optInt("stylesheets", 0),
                        images = json.optInt("images", 0),
                        links = json.optInt("links", 0),
                        metaDescription = json.optString("metaDesc", "—"),
                        charset = json.optString("charset", "—")
                    )
                )
            } catch (e: Exception) {
                onResult(PageInfo())
            }
        }
    }
}

data class PageInfo(
    val title: String = "—",
    val url: String = "—",
    val scripts: Int = 0,
    val stylesheets: Int = 0,
    val images: Int = 0,
    val links: Int = 0,
    val metaDescription: String = "—",
    val charset: String = "—"
)
