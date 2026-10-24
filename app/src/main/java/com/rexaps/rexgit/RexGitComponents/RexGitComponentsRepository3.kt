
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.only

@Composable
internal fun GitDevGuide() {
    GuideHeading("What GitDev mode does")
    GuideText(
        "Push writes commit messages like  [26.57] Fix: login: your description.  " +
            "The word after the type is a folder of your app. GitDev tells RexGit " +
            "where your app's code lives, so it works for any APK project, not " +
            "only com/rexaps."
    )
    GuideText(
        "Normal Push works without GitDev. Enable the optional GitDev metadata mode " +
            "when you want package-folder scopes, version bumps, and structured commit prefixes. " +
            "Settings are saved per repository."
    )

    GuideHeading("How folders are detected")
    GuideText(
        "RexGit opens  <module>/src/main/java/<package path>  and " +
            " <module>/src/main/kotlin/<package path>.  Every folder directly " +
            "inside becomes a choice in the Push dialog. Hidden folders (starting " +
            "with a dot) are ignored, and so is MainActivity.kt. A folder that " +
            "appears in your changed files is added too."
    )

    GuideHeading("Option 1: Package path (required)")
    GuideText(
        "The folder that CONTAINS your feature folders, written as a path " +
            "starting from the first folder after java or kotlin."
    )
    GuideText(
        "Tip: open any .kt file. If it starts with  package com.rexaps.login  " +
            "then login is the feature folder and your package path is  com/rexaps. " +
            "Dots work too: com.rexaps"
    )

    GuideHeading("Option 2: Module folder (optional)")
    GuideText(
        "The Gradle module that holds the code. Leave it empty and RexGit tries " +
            "app/ first, then the project root. Fill it in only for another " +
            "module, for example  feature-login."
    )

    GuideHeading("Example")
    GuideCode(
        "app/src/main/java/com/rexaps/\n" +
            "  login/          -> folder choice: login\n" +
            "  settings/       -> folder choice: settings\n" +
            "  MainActivity.kt -> ignored\n\n" +
            "Package path: com/rexaps"
    )

    GuideHeading("Other apps")
    GuideText("Not every APK uses com/rexaps. Other examples:")
    GuideCode(
        "id/mycompany/notes\n" +
            "org/example/tools\n" +
            "io/github/yourname/app"
    )

    GuideHeading("Before you activate")
    GuideCode(
        "1. Open your repository in RexGit.\n" +
            "2. Browse app > src > main > java (or kotlin).\n" +
            "3. Follow the folders (com > yourname ...) until you reach the\n" +
            "   folder that holds your feature folders.\n" +
            "4. Type that path as the package path.\n" +
            "5. RexGit lists the folders it found. An empty list means the\n" +
            "   path (or module) is wrong."
    )
}

@Composable
internal fun RexPushLoader(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "rexPushLoader")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scheme = MaterialTheme.colorScheme

    Canvas(Modifier.size(104.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.34f

        drawCircle(
            color = scheme.outlineVariant.copy(alpha = 0.45f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx()
            )
        )

        repeat(8) { index ->
            val angle = Math.toRadians(
                (rotation + index * 45f).toDouble()
            )
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            val emphasis = if (index == 0) 1f else 0.28f
            drawCircle(
                color = scheme.primary.copy(
                    alpha = if (active) emphasis * pulse else 0.25f
                ),
                radius = if (index == 0) 5.dp.toPx() else 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

@Composable
internal fun RexGitLogo() {
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Code,
            contentDescription = null,
            tint = scheme.onPrimary
        )
    }
}
