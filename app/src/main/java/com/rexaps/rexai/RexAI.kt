package com.rexaps.rexai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * RexAI v1: a lightweight hub for official AI websites.
 *
 * Provider conversations run on the provider's website. RexAps does not collect
 * provider credentials, tokens, prompts, or conversation contents.
 */
enum class RexAIProvider(
    val id: String,
    val title: String,
    val subtitle: String,
    val homeUrl: String,
    val domains: Set<String>
) {
    CHATGPT(
        id = "chatgpt",
        title = "ChatGPT",
        subtitle = "Chat with OpenAI",
        homeUrl = "https://chatgpt.com/",
        domains = setOf("chatgpt.com", "openai.com", "auth0.com")
    ),
    CLAUDE(
        id = "claude",
        title = "Claude",
        subtitle = "Think with Anthropic",
        homeUrl = "https://claude.ai/",
        domains = setOf("claude.ai", "anthropic.com")
    ),
    GEMINI(
        id = "gemini",
        title = "Gemini",
        subtitle = "Explore Google's AI",
        homeUrl = "https://gemini.google.com/",
        domains = setOf("gemini.google.com", "google.com", "googleapis.com", "gstatic.com", "googleusercontent.com")
    );

    fun allowsHost(host: String?): Boolean {
        val normalized = host?.lowercase()?.trimEnd('.') ?: return false
        return domains.any { normalized == it || normalized.endsWith(".$it") }
    }
}

@Composable
fun RexAIScreen() {
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedProvider = RexAIProvider.entries.firstOrNull { it.id == selectedProviderId }

    BackHandler(enabled = selectedProvider != null) {
        selectedProviderId = null
    }

    when (selectedProvider) {
        RexAIProvider.CHATGPT -> ChatGPTScreen(onBack = { selectedProviderId = null })
        RexAIProvider.CLAUDE -> ClaudeScreen(onBack = { selectedProviderId = null })
        RexAIProvider.GEMINI -> GeminiScreen(onBack = { selectedProviderId = null })
        null -> RexAIHomeScreen(
            onSelectProvider = { selectedProviderId = it.id }
        )
    }
}

@Composable
private fun RexAIHomeScreen(
    onSelectProvider: (RexAIProvider) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp)
            .padding(top = 18.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primary, colors.tertiary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RexAI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI workspace",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = colors.primaryContainer
            ) {
                Text(
                    text = "V1",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = colors.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.22f),
                            colors.tertiary.copy(alpha = 0.13f),
                            colors.surfaceVariant.copy(alpha = 0.78f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your AI, in one place.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "Pilih platform AI untuk membuka layanan resminya. Login dan percakapan dikelola langsung oleh masing-masing provider.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RexAIInfoPill("3 provider")
                    RexAIInfoPill("Web interface")
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "AI PROVIDERS",
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 250.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(RexAIProvider.entries.toList(), key = { it.id }) { provider ->
                RexAIProviderCard(
                    provider = provider,
                    onClick = { onSelectProvider(provider) }
                )
            }

            item(key = "agent-coming-soon") {
                RexAIAgentComingSoonCard()
            }
        }

        Text(
            text = "RexAI v1 • AI Agent sedang disiapkan",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun RexAIProviderCard(
    provider: RexAIProvider,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val icon: ImageVector = when (provider) {
        RexAIProvider.CHATGPT -> Icons.Outlined.AutoAwesome
        RexAIProvider.CLAUDE -> Icons.Outlined.Psychology
        RexAIProvider.GEMINI -> Icons.Outlined.TipsAndUpdates
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primary, colors.tertiary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = provider.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "OPEN PROVIDER  →",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RexAIAgentComingSoonCard() {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.65f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colors.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Agent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Automasi dan tool integration",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(7.dp))
                RexAIInfoPill("SEGERA HADIR")
            }
        }
    }
}

@Composable
private fun RexAIInfoPill(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface.copy(alpha = 0.72f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
