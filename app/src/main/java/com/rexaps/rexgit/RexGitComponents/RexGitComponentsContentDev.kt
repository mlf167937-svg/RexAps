@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.only

@Composable
internal fun GuideCode(text: String) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = scheme.onSurfaceVariant,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}

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

// ============================================================
// Shared building blocks
// ============================================================

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

@Composable
internal fun IconTile(
    icon: ImageVector,
    tint: Color,
    size: Dp = 40.dp
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Reserves space for the progress bar so the layout never jumps. */
@Composable
internal fun ProgressSlot(visible: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        if (visible) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(10.dp))

        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search")
            }
        }
        if (onSearch != null) {
            IconButton(onClick = onSearch, enabled = value.isNotBlank()) {
                Icon(Icons.Default.Search, contentDescription = "Search GitHub")
            }
        } else if (value.isEmpty()) {
            Spacer(Modifier.width(10.dp))
        }
    }
}
