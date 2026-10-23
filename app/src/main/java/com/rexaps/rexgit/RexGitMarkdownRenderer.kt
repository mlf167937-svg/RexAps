package com.rexaps.rexgit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.text.Layout
import android.text.style.AlignmentSpan
import android.view.View
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolver
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.destination.ImageDestinationProcessorRelativeToAbsolute
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.prism4j.Prism4j
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Native Markdown renderer for README and Release notes.
 * Uses Markwon (no WebView), so README HTML is rendered as native spans and
 * scripts are never executed. Only web/mail links are opened externally.
 */
@Composable
internal fun RexGitMarkdownRichText(
    content: String,
    rawBaseUrl: String = "",
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onInternalLink: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val latestInternalLink = rememberUpdatedState(onInternalLink)
    val foreground = if (darkTheme) Color(0xFFE6EDF3) else Color(0xFF24292F)
    val linkColor = if (darkTheme) Color(0xFF79C0FF) else Color(0xFF0969DA)

    val markwon = remember(context, rawBaseUrl, darkTheme) {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create { plugin ->
                plugin.addHandler(GitHubAlignmentTagHandler())
            })
            .usePlugin(ImagesPlugin.create(object : ImagesPlugin.ImagesConfigure {
                override fun configureImages(plugin: ImagesPlugin) {
                    plugin.placeholderProvider(object : ImagesPlugin.PlaceholderProvider {
                        override fun providePlaceholder(drawable: AsyncDrawable): android.graphics.drawable.Drawable? {
                            val size = (context.resources.displayMetrics.density * 48f).toInt().coerceAtLeast(24)
                            return ColorDrawable(if (darkTheme) 0xFF21262D.toInt() else 0xFFF0F2F4.toInt()).apply {
                                setBounds(0, 0, size, size)
                            }
                        }
                    })
                }
            }))
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(
                SyntaxHighlightPlugin.create(
                    Prism4j(RexGitGrammarLocator()),
                    if (darkTheme) Prism4jThemeDarkula.create() else Prism4jThemeDefault.create()
                )
            )
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    if (rawBaseUrl.isNotBlank()) {
                        builder.imageDestinationProcessor(
                            ImageDestinationProcessorRelativeToAbsolute(rawBaseUrl)
                        )
                    }
                    builder.linkResolver(object : LinkResolver {
                        override fun resolve(view: View, link: String) {
                            val uri = try { Uri.parse(link) } catch (_: Throwable) { return }
                            when (uri.scheme?.lowercase(Locale.ROOT)) {
                                "http", "https", "mailto" -> {
                                    try {
                                        view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Throwable) {
                                        // A missing browser/mail app should not crash repository preview.
                                    }
                                }
                                null, "" -> latestInternalLink.value(link)
                                else -> Unit // Disallow javascript:, intent:, file:, and unknown schemes.
                            }
                        }
                    })
                }
            })
            .build()
    }

    val renderedContent = remember(content) { addMarkdownCodeLanguageLabels(content) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 14f
                setTextColor(foreground.toArgb())
                setLinkTextColor(linkColor.toArgb())
                setLineSpacing(resources.displayMetrics.density * 3f, 1.0f)
                setPadding(
                    (resources.displayMetrics.density * 2f).toInt(),
                    (resources.displayMetrics.density * 4f).toInt(),
                    (resources.displayMetrics.density * 2f).toInt(),
                    (resources.displayMetrics.density * 4f).toInt()
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { view ->
            view.setTextColor(foreground.toArgb())
            view.setLinkTextColor(linkColor.toArgb())
            markwon.setMarkdown(view, renderedContent)
        }
    )
}

/** Adds a small Markdown label before known-language fenced blocks, without changing code contents. */
private fun addMarkdownCodeLanguageLabels(markdown: String): String {
    val fence = Regex("""(?m)^([ \t]{0,3})(`{3,}|~{3,})[ \t]*([A-Za-z][A-Za-z0-9_+.#-]*)(?:[ \t]+[^\r\n]*)?$""")
    val labels = mapOf(
        "kt" to "Kotlin", "kts" to "Kotlin", "kotlin" to "Kotlin",
        "js" to "JavaScript", "javascript" to "JavaScript", "ts" to "TypeScript",
        "java" to "Java", "json" to "JSON", "html" to "HTML", "xml" to "XML",
        "css" to "CSS", "yaml" to "YAML", "yml" to "YAML", "py" to "Python",
        "python" to "Python", "sql" to "SQL", "go" to "Go", "swift" to "Swift",
        "sh" to "Shell", "bash" to "Shell", "md" to "Markdown", "markdown" to "Markdown"
    )
    return fence.replace(markdown) { match ->
        val info = match.groupValues[3].lowercase(Locale.ROOT)
        val label = labels[info] ?: return@replace match.value
        "${match.groupValues[1]}**$label**\n\n${match.value}"
    }
}

/** Supports GitHub README alignment without allowing arbitrary HTML/CSS execution. */
private class GitHubAlignmentTagHandler : SimpleTagHandler() {
    override fun getSpans(
        configuration: MarkwonConfiguration,
        renderProps: RenderProps,
        tag: HtmlTag
    ): Any? {
        val attributes = tag.attributes()
        val styleAlign = attributes["style"]
            ?.let { Regex("(?:^|;)\\s*text-align\\s*:\\s*(left|right|center|start|end)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.getOrNull(1) }
        val align = (attributes["align"] ?: styleAlign).orEmpty().lowercase(Locale.ROOT)
        val layoutAlignment = when (align) {
            "center" -> Layout.Alignment.ALIGN_CENTER
            "right", "end" -> Layout.Alignment.ALIGN_OPPOSITE
            "left", "start" -> Layout.Alignment.ALIGN_NORMAL
            else -> return null
        }
        return AlignmentSpan.Standard(layoutAlignment)
    }

    override fun supportedTags(): Collection<String> = listOf("p", "div", "center")
}

/** Resolves local Markdown image paths against the selected repository's default branch. */
internal fun githubRawReadmeBase(repo: RexGitRepository): String {
    val remote = repo.remoteUrl.orEmpty()
    val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
    val owner = match?.groupValues?.getOrNull(1) ?: repo.globalOwner ?: repo.accountNickname ?: return ""
    val repository = match?.groupValues?.getOrNull(2)?.removeSuffix(".git") ?: repo.name
    val branch = repo.branch.ifBlank { "main" }.split('/').joinToString("/") { part ->
        java.net.URLEncoder.encode(part, Charsets.UTF_8.name()).replace("+", "%20")
    }
    return "https://raw.githubusercontent.com/$owner/$repository/$branch/"
}

/** Opens repository-relative Markdown links in RexGit's existing file editor, while blocking path traversal. */
internal fun openReadmeFileLink(
    repo: RexGitRepository,
    link: String,
    onOpenFile: (RexGitFile) -> Unit
) {
    val pathOnly = link.substringBefore('#').substringBefore('?').trim()
    if (pathOnly.isBlank() || pathOnly.startsWith('/') || pathOnly.contains("://")) return
    val relative = Uri.decode(pathOnly.removePrefix("./")).removePrefix("/")
    if (relative.isBlank()) return
    try {
        val root = java.io.File(repo.path).canonicalFile
        val target = java.io.File(root, relative).canonicalFile
        val rootPrefix = root.path.trimEnd(java.io.File.separatorChar) + java.io.File.separator
        if (!target.path.startsWith(rootPrefix) || !target.isFile || !target.exists()) return
        onOpenFile(
            RexGitFile(
                name = target.name,
                path = target.absolutePath,
                isDirectory = false,
                size = target.length()
            )
        )
    } catch (_: Throwable) {
        // Malformed paths and inaccessible files are ignored rather than crashing the preview.
    }
}


@Composable
internal fun RexGitRepositoryAvatar(
    url: String?,
    fallback: String,
    modifier: Modifier = Modifier
) {
    val bitmap = produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = if (url.isNullOrBlank() || !url.startsWith("https://", ignoreCase = true)) {
            null
        } else withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.getInputStream().use { BitmapFactory.decodeStream(it) }
            } catch (_: Throwable) {
                null
            }
        }
    }.value

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$fallback GitHub avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(15.dp))
        )
    } else {
        androidx.compose.material3.Surface(
            modifier = modifier,
            shape = RoundedCornerShape(15.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.Text(
                    fallback.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
