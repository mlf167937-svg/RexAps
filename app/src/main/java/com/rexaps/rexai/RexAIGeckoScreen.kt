package com.rexaps.rexai

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mozilla.geckoview.GeckoView

@Composable
internal fun RexAIGeckoScreen(
    providerId: String,
    title: String,
    startUrl: String,
    holder: RexAIWebStateHolder,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val pages by holder.pages.collectAsStateWithLifecycle()
    val page = pages[providerId] ?: holder.page(providerId)
    val session = remember(providerId) { holder.getOrCreateSession(context, providerId, startUrl) }

    DisposableEffect(providerId, session) {
        holder.setProviderActive(providerId, true)
        session.setFocused(true)
        onDispose {
            session.setFocused(false)
            holder.setProviderActive(providerId, false)
        }
    }

    val navigateBack: () -> Unit = {
        if (page.canGoBack) holder.goBack(providerId) else onBack()
    }
    BackHandler(enabled = true) { navigateBack() }

    Column(Modifier.fillMaxSize()) {
        Surface(color = colors.surface, tonalElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = navigateBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = if (page.canGoBack) "Back in page" else "Back to RexAI")
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        Uri.parse(page.url.ifBlank { startUrl }).host ?: "Official website",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = { holder.reload(context, providerId, startUrl) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
                }
                IconButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page.url.ifBlank { startUrl }))) }
                }) {
                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Open in external browser")
                }
            }
        }
        if (page.isLoading) {
            LinearProgressIndicator(
                progress = { page.progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { viewContext ->
                    GeckoView(viewContext).apply {
                        setSession(session)
                        session.setActive(true)
                        session.setFocused(true)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view -> view.releaseSession() }
            )
            if (page.error != null) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(22.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Page unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(page.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Surface(
                            onClick = { holder.reload(context, providerId, startUrl) },
                            color = colors.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text("Try again", modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = colors.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (page.isLoading && page.progress == 0) {
                CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), strokeWidth = 2.dp)
            }
        }
    }
}
