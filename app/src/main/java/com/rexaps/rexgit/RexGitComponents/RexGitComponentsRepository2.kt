@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_GithubRepositoryCard(
    repo: RexGitGithubRepository,
    cloned: Boolean,
    cloning: Boolean,
    anyCloning: Boolean,
    onClone: () -> Unit,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Default.Code, scheme.secondary)

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        repo.fullName,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                InfoPill(
                    icon = if (repo.private) Icons.Default.Lock else Icons.Default.Public,
                    text = if (repo.private) "Private" else "Public",
                    container = scheme.surfaceVariant,
                    content = scheme.onSurfaceVariant,
                    compact = true
                )
            }

            if (!repo.description.isNullOrBlank()) {
                Text(
                    repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (cloned) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open")
                }
            } else {
                FilledTonalButton(
                    onClick = onClone,
                    enabled = !anyCloning,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    if (cloning) ButtonSpinner()
                    else Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))
                    Text(if (cloning) "Cloning" else "Clone")
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_GitDevGuide() {
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
