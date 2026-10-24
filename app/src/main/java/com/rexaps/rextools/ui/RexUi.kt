// RexUi.kt
package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexScreen(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    scroll: Boolean = true,
    padded: Boolean = true,
    toast: RexToastState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scroll && scrollState.value > 8 } }
    val barColor by animateColorAsState(
        if (scrolled) s.background.copy(alpha = 0.88f) else Color.Transparent, tween(250), label = "bar"
    )
    val lineAlpha by animateFloatAsState(if (scrolled) 0.7f else 0f, tween(250), label = "line")

    RexBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            AnimatedContent(
                                targetState = title to subtitle,
                                transitionSpec = {
                                    (fadeIn(tween(240, delayMillis = 80)) + slideInVertically(tween(300, easing = RexEase)) { it / 3 }) togetherWith
                                        fadeOut(tween(120))
                                },
                                label = "title"
                            ) { (t, sub) ->
                                Column {
                                    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (sub != null) {
                                        Text(sub, style = MaterialTheme.typography.labelMedium, color = s.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        navigationIcon = { RexBackButton(onBack) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barColor,
                            scrolledContainerColor = barColor
                        )
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(s.outlineVariant.copy(alpha = lineAlpha)))
                }
            }
        ) { pad ->
            var m = Modifier.fillMaxSize().padding(pad)
            if (scroll) m = m.verticalScroll(scrollState)
            m = m.entrance(0, offset = 16.dp)
            if (padded) m = m.padding(horizontal = 20.dp, vertical = 8.dp)
            Column(
                modifier = m,
                verticalArrangement = if (padded) Arrangement.spacedBy(16.dp) else Arrangement.Top,
                content = content
            )
        }
        if (toast != null) RexToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }
}
