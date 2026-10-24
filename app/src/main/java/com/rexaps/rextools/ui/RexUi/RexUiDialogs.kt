package com.rexaps.rextools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun rememberRexToast(): RexToastState = remember { RexToastState() }

@Composable
fun RexToastHost(state: RexToastState, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    LaunchedEffect(state.tick) {
        if (state.message != null) {
            delay(2600)
            state.dismiss()
        }
    }
    var last by remember { mutableStateOf("") }
    SideEffect { state.message?.let { last = it } }
    val accent = if (state.isError) s.error else s.secondary

    AnimatedVisibility(
        visible = state.message != null,
        modifier = modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        enter = slideInVertically(spring(0.7f, Spring.StiffnessMediumLow)) { it * 2 } + fadeIn() + scaleIn(initialScale = 0.85f),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)) + scaleOut(targetScale = 0.9f)
    ) {
        Row(
            Modifier
                .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
                .clip(CircleShape)
                .background(s.surfaceContainerHighest)
                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (state.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)
            )
            Text(state.message ?: last, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 2)
        }
    }
}
