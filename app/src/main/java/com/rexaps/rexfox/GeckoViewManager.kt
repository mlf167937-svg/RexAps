package com.rexaps.rexfox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

/** Owns one GeckoSession's callbacks. Runtime is shared for the application process. */
class GeckoViewManager(
    private val activity: Activity,
    private val runtime: GeckoRuntime,
    private val settingsProvider: () -> BrowserSettings,
    private val onState: (url: String, title: String, progress: Int, loading: Boolean, error: String?) -> Unit,
    private val onNewWindow: (String) -> GeckoSession,
    private val onNavigationCapabilities: (Boolean?, Boolean?) -> Unit,
    private val onDownload: (String, String?, String?, String?) -> Unit
) {
    fun createSession(incognito: Boolean): GeckoSession {
        val settings = settingsProvider()
        val builder = GeckoSessionSettings.Builder()
            .allowJavascript(settings.javaScriptEnabled)
            .usePrivateMode(incognito)
            .useTrackingProtection(settings.trackingProtectionEnabled)
            .userAgentMode(if (settings.desktopSite) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(if (settings.desktopSite) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
        val session = GeckoSession(builder.build())
        var currentUrl = "about:blank"
        var currentTitle = "New Tab"
        session.open(runtime)

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                currentUrl = url.orEmpty()
                onState(currentUrl, currentTitle, 0, true, null)
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                onNavigationCapabilities(canGoBack, null)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                onNavigationCapabilities(null, canGoForward)
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = Uri.parse(request.uri)
                val scheme = uri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob") return null
                openExternal(uri)
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? =
                GeckoResult.fromValue(onNewWindow(uri))

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: org.mozilla.geckoview.WebRequestError
            ): GeckoResult<String>? {
                currentUrl = uri.orEmpty()
                onState(currentUrl, currentTitle, 0, false, "Unable to load this page (${error.code}).")
                return null
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                currentUrl = url
                onState(currentUrl, currentTitle, 0, true, null)
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                onState(currentUrl, currentTitle, progress, progress < 100, null)
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                onState(currentUrl, currentTitle, 100, false,
                    if (success) null else "The page could not be loaded.")
            }
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title.orEmpty()
                onState(currentUrl, currentTitle, 100, false, null)
            }
            override fun onCloseRequest(session: GeckoSession) {
                // The tab manager owns closing; a page close request is intentionally not allowed
                // to close the whole app.
            }
            override fun onExternalResponse(session: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                onDownload(response.uri, null, null, response.headers["Content-Type"])
                runCatching { response.body?.close() }
            }
        }
        return session
    }

    private fun openExternal(uri: Uri) {
        runCatching {
            val intent = if (uri.scheme.equals("intent", true)) {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            } else Intent(Intent.ACTION_VIEW, uri)
            ContextCompat.startActivity(activity, intent, null)
        }
    }
}
