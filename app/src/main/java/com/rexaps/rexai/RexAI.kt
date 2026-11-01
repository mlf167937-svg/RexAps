package com.rexaps.rexai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** RexAI v1: curated provider WebViews. Agent functionality is intentionally marked as upcoming. */
@Composable
fun RexAIScreen(onExit: () -> Unit = {}) {
    val webStateHolder: RexAIWebStateHolder = viewModel()
    var activeProvider by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = activeProvider != null) {
        activeProvider = null
    }

    when (activeProvider) {
        "chatgpt" -> ChatGPTScreen(holder = webStateHolder, onBack = { activeProvider = null })
        "claude" -> ClaudeScreen(holder = webStateHolder, onBack = { activeProvider = null })
        "gemini" -> GeminiScreen(holder = webStateHolder, onBack = { activeProvider = null })
        else -> RexAIHomeScreen(onExit = onExit, onSelectProvider = { activeProvider = it })
    }
}

@Composable
private fun RexAIHomeScreen(onExit: () -> Unit, onSelectProvider: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onExit) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to RexAps")
            }
            Surface(shape = RoundedCornerShape(16.dp), color = colors.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("RexAI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Your AI workspace", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = colors.secondaryContainer) {
                Text("V1", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier.background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))).padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One space. Multiple minds.", color = colors.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Buka layanan AI pilihanmu langsung dari RexAps. Setiap provider memakai situs resminya dan mengelola login melalui alur yang didukung layanan tersebut.",
                        color = colors.onPrimary.copy(alpha = .88f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI PROVIDERS", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
            ProviderCard(
                title = "ChatGPT",
                subtitle = "OpenAI · Chat and create",
                initials = "GPT",
                tint = Color(0xFF10A37F),
                description = "Buka ChatGPT Web",
                onClick = { onSelectProvider("chatgpt") }
            )
            ProviderCard(
                title = "Claude",
                subtitle = "Anthropic · Think and write",
                initials = "C",
                tint = Color(0xFFD97757),
                description = "Buka Claude Web",
                onClick = { onSelectProvider("claude") }
            )
            ProviderCard(
                title = "Gemini",
                subtitle = "Google · Explore and learn",
                initials = "G",
                tint = Color(0xFF4285F4),
                description = "Buka Gemini Web",
                onClick = { onSelectProvider("gemini") }
            )
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = .58f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Surface(shape = RoundedCornerShape(15.dp), color = colors.secondaryContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(25.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("AI Agent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Workflow automation and tools", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = colors.tertiaryContainer) {
                    Text("SEGERA", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = colors.onTertiaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "RexAI v1 uses official provider websites. Google sign-in may open your external browser because providers can restrict OAuth inside embedded WebViews. Account access and session sharing depend on each provider.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProviderCard(
    title: String,
    subtitle: String,
    initials: String,
    tint: Color,
    description: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .75f)),
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(tint.copy(alpha = .15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(description, style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            Icon(Icons.Rounded.Bolt, contentDescription = "Open $title", tint = colors.primary)
        }
    }
}
