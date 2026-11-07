package com.rexaps.rexai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.rexaps.gecko.RexGeckoRuntime
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebRequestError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds persistent, provider-isolated GeckoSessions across RexAI provider-screen changes. */
data class RexAIPageState(
    val url: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = true,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: String? = null
)

class RexAIWebStateHolder : ViewModel() {
    private val sessions = linkedMapOf<String, GeckoSession>()
    private val opened = mutableSetOf<String>()
    private val _pages = MutableStateFlow<Map<String, RexAIPageState>>(emptyMap())
    val pages = _pages.asStateFlow()

    fun getOrCreateSession(context: Context, providerId: String, startUrl: String): GeckoSession {
        val existing = sessions[providerId]
        if (existing != null) {
            if (!existing.isOpen) {
                existing.open(RexGeckoRuntime.get(context))
                val restoreUrl = _pages.value[providerId]?.url?.ifBlank { startUrl } ?: startUrl
                existing.load(GeckoSession.Loader().uri(restoreUrl))
            } else if (providerId !in opened) {
                // A content-process crash can leave the session open but without a live page.
                existing.reload()
            }
            opened += providerId
            existing.setActive(true)
            return existing
        }

        val settings = GeckoSessionSettings.Builder()
            .allowJavascript(true)
            .usePrivateMode(false)
            // Dedicated persistent profile per AI provider: no cross-provider cookie/local-storage sharing.
            .contextId("rexai-$providerId")
            .build()
        val session = GeckoSession(settings)
        sessions[providerId] = session
        _pages.value = _pages.value + (providerId to RexAIPageState(url = startUrl))
        attachDelegates(context.applicationContext, providerId, session)
        session.open(RexGeckoRuntime.get(context))
        opened += providerId
        session.load(GeckoSession.Loader().uri(startUrl))
        session.setActive(true)
        return session
    }

    private fun attachDelegates(context: Context, providerId: String, session: GeckoSession) {
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = runCatching { Uri.parse(request.uri) }.getOrNull() ?: return null
                val scheme = uri.scheme.orEmpty().lowercase()
                val host = uri.host.orEmpty().lowercase()
                if (scheme !in setOf("http", "https")) {
                    openExternal(context, uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                // Provider authentication hosts are explicitly allowlisted below. This keeps
                // Gemini/Google SSO inside its isolated provider context without allowing arbitrary
                // Google pages as a navigation destination.
                if (isProviderHost(providerId, host)) return null
                openExternal(context, uri)
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                update(providerId) { it.copy(url = url.orEmpty().ifBlank { it.url }, error = null) }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                update(providerId) { it.copy(canGoBack = canGoBack) }
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                update(providerId) { it.copy(canGoForward = canGoForward) }
            }

            override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
                val isCertificateError = error.category == WebRequestError.ERROR_CATEGORY_SECURITY
                update(providerId) {
                    it.copy(
                        isLoading = false,
                        error = if (isCertificateError) {
                            "Secure connection could not be verified. Certificate errors are blocked. Check the network and retry."
                        } else {
                            "Unable to load page (${error.message ?: "network error"})."
                        }
                    )
                }
                // Do not create certificate exceptions or override Gecko's TLS validation.
                return null
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? = null
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                update(providerId) { it.copy(url = url, progress = 0, isLoading = true, error = null) }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                update(providerId) { it.copy(progress = progress.coerceIn(0, 100), isLoading = progress < 100) }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                update(providerId) { it.copy(progress = if (success) 100 else it.progress, isLoading = false) }
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onCrash(session: GeckoSession) {
                opened.remove(providerId)
                update(providerId) {
                    it.copy(isLoading = false, canGoBack = false, canGoForward = false,
                        error = "The browser content process stopped. Retry to restore this page.")
                }
            }

            override fun onKill(session: GeckoSession) {
                opened.remove(providerId)
                update(providerId) {
                    it.copy(isLoading = false, canGoBack = false, canGoForward = false,
                        error = "The browser content process was stopped by Android. Retry to reopen it.")
                }
            }
        }
    }

    fun setProviderActive(providerId: String, active: Boolean) {
        sessions[providerId]?.setActive(active)
    }

    fun goBack(providerId: String) { sessions[providerId]?.goBack() }
    fun goForward(providerId: String) { sessions[providerId]?.goForward() }

    fun reload(context: Context, providerId: String, startUrl: String) {
        val session = sessions[providerId] ?: return
        if (!session.isOpen) {
            session.open(RexGeckoRuntime.get(context))
            opened += providerId
            val restoreUrl = _pages.value[providerId]?.url?.ifBlank { startUrl } ?: startUrl
            session.load(GeckoSession.Loader().uri(restoreUrl))
        } else session.reload()
        update(providerId) { it.copy(isLoading = true, error = null) }
    }

    fun page(providerId: String): RexAIPageState = _pages.value[providerId] ?: RexAIPageState()

    private fun update(providerId: String, transform: (RexAIPageState) -> RexAIPageState) {
        val current = _pages.value[providerId] ?: RexAIPageState()
        _pages.value = _pages.value + (providerId to transform(current))
    }

    override fun onCleared() {
        sessions.values.forEach { session -> runCatching { session.setActive(false); session.close() } }
        sessions.clear()
        opened.clear()
        super.onCleared()
    }

    private fun openExternal(context: Context, uri: Uri) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun isProviderHost(providerId: String, host: String): Boolean = when (providerId) {
        "chatgpt" -> host == "chatgpt.com" || host.endsWith(".chatgpt.com") || host == "openai.com" || host.endsWith(".openai.com") || host.endsWith(".oaistatic.com") || host.endsWith(".oaiusercontent.com") || host == "accounts.google.com"
        "claude" -> host == "claude.ai" || host.endsWith(".claude.ai") || host == "anthropic.com" || host.endsWith(".anthropic.com") || host == "accounts.google.com"
        "gemini" -> host == "gemini.google.com" || host == "google.com" || host.endsWith(".google.com") || host.endsWith(".googleusercontent.com") || host.endsWith(".gstatic.com")
        else -> false
    }
}
