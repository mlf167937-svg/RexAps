package com.rexaps.rexfox

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider

enum class DevToolsTab { SOURCE, INFO, DOWNLOAD }

@Composable
fun DevToolsScreen(
    html: String,
    pageInfo: PageInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(DevToolsTab.SOURCE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070F))
    ) {
        // ── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F1A))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = Cyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "DevTools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfacePrimary
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close", tint = OnSurfaceMuted)
            }
        }

        // ── Tab Row ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F1A))
                .padding(horizontal = 12.dp)
        ) {
            DevToolsTab.values().forEach { tab ->
                val selected = tab == selectedTab
                val tint by animateColorAsState(
                    if (selected) Cyan else OnSurfaceMuted, label = "tint"
                )
                TextButton(onClick = { selectedTab = tab }) {
                    Text(
                        text = tab.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        color = tint,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(60.dp)
                            .background(Cyan)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E1E2E))

        // ── Content ──────────────────────────────────────────────
        AnimatedContent(targetState = selectedTab, label = "tab") { tab ->
            when (tab) {
                DevToolsTab.SOURCE -> HtmlSourcePanel(html)
                DevToolsTab.INFO -> PageInfoPanel(pageInfo)
                DevToolsTab.DOWNLOAD -> DownloadPanel(context, html, pageInfo.url)
            }
        }
    }
}

// ── HTML Source viewer with basic syntax highlighting ─────────────────────────

@Composable
fun HtmlSourcePanel(html: String) {
    val scrollV = rememberScrollState()
    val scrollH = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0A14))
    ) {
        Text(
            text = syntaxHighlight(html),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollV)
                .horizontalScroll(scrollH)
                .padding(14.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

fun syntaxHighlight(html: String) = buildAnnotatedString {
    val tagColor = Color(0xFF7DD3FC)      // blue — tags
    val attrColor = Color(0xFFA78BFA)     // violet — attributes
    val valueColor = Color(0xFF86EFAC)    // green — values
    val commentColor = Color(0xFF6B7280)  // grey — comments
    val textColor = Color(0xFFE2E8F0)     // default

    var i = 0
    while (i < html.length) {
        when {
            // HTML comments
            html.startsWith("<!--", i) -> {
                val end = html.indexOf("-->", i).takeIf { it != -1 }?.plus(3) ?: html.length
                withStyle(SpanStyle(color = commentColor)) { append(html.substring(i, end)) }
                i = end
            }
            // Tags
            html[i] == '<' -> {
                val end = html.indexOf('>', i).takeIf { it != -1 }?.plus(1) ?: html.length
                val tag = html.substring(i, end)
                // Split tag name vs attributes for colouring
                val spaceIdx = tag.indexOfFirst { it == ' ' || it == '>' }
                val tagPart = tag.substring(0, spaceIdx.coerceAtLeast(1))
                val rest = tag.substring(tagPart.length)
                withStyle(SpanStyle(color = tagColor)) { append(tagPart) }
                // Colour attribute names vs values inside the rest
                var j = 0
                while (j < rest.length) {
                    when {
                        rest[j] == '"' -> {
                            val q = rest.indexOf('"', j + 1).takeIf { it != -1 }?.plus(1) ?: rest.length
                            withStyle(SpanStyle(color = valueColor)) { append(rest.substring(j, q)) }
                            j = q
                        }
                        rest[j] == '\'' -> {
                            val q = rest.indexOf('\'', j + 1).takeIf { it != -1 }?.plus(1) ?: rest.length
                            withStyle(SpanStyle(color = valueColor)) { append(rest.substring(j, q)) }
                            j = q
                        }
                        rest[j] == '>' || rest[j] == '/' -> {
                            withStyle(SpanStyle(color = tagColor)) { append(rest[j]) }
                            j++
                        }
                        rest[j].isLetter() || rest[j] == '-' -> {
                            var k = j
                            while (k < rest.length && rest[k] != '=' && rest[k] != ' ' && rest[k] != '>' && rest[k] != '/') k++
                            withStyle(SpanStyle(color = attrColor)) { append(rest.substring(j, k)) }
                            j = k
                        }
                        else -> { append(rest[j]); j++ }
                    }
                }
                i = end
            }
            else -> {
                var j = i
                while (j < html.length && html[j] != '<') j++
                withStyle(SpanStyle(color = textColor)) { append(html.substring(i, j)) }
                i = j
            }
        }
    }
}

// ── Page Info panel ───────────────────────────────────────────────────────────

@Composable
fun PageInfoPanel(info: PageInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Page Inspector",
            style = MaterialTheme.typography.titleSmall,
            color = Cyan,
            fontWeight = FontWeight.Bold
        )

        InfoRow("Title", info.title)
        InfoRow("URL", info.url)
        InfoRow("Charset", info.charset)
        InfoRow("Meta Description", info.metaDescription)

        HorizontalDivider(color = Color(0xFF1E1E2E), modifier = Modifier.padding(vertical = 4.dp))

        Text(
            "Asset Summary",
            style = MaterialTheme.typography.titleSmall,
            color = Cyan,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip(modifier = Modifier.weight(1f), label = "Scripts", value = info.scripts)
            StatChip(modifier = Modifier.weight(1f), label = "Styles", value = info.stylesheets)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip(modifier = Modifier.weight(1f), label = "Images", value = info.images)
            StatChip(modifier = Modifier.weight(1f), label = "Links", value = info.links)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .padding(12.dp)
    ) {
        Text(label, fontSize = 10.sp, color = OnSurfaceMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            value.ifBlank { "—" },
            fontSize = 13.sp,
            color = OnSurfacePrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StatChip(modifier: Modifier = Modifier, label: String, value: Int) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VioletLight
        )
        Text(label, fontSize = 11.sp, color = OnSurfaceMuted)
    }
}

// ── Download panel ────────────────────────────────────────────────────────────

@Composable
fun DownloadPanel(context: Context, html: String, pageUrl: String) {
    var saved by remember { mutableStateOf(false) }
    var savedPath by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Download, null, tint = Cyan, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Save Page Source",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfacePrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Save the full live DOM of this page as an HTML file",
            color = OnSurfaceMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(28.dp))

        if (!saved) {
            Button(
                onClick = {
                    val file = DevTools.saveHtmlToFile(context, html)
                    savedPath = file.absolutePath
                    saved = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save as page_source.html")
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0A2010)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✅  Saved!", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(savedPath, fontSize = 11.sp, color = OnSurfaceMuted, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    val file = DevTools.saveHtmlToFile(context, html)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/html"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share HTML"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan)
            ) {
                Icon(Icons.Default.Share, null, tint = Cyan)
                Spacer(Modifier.width(8.dp))
                Text("Share file", color = Cyan)
            }
        }
    }
}
