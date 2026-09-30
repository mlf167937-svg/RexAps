package com.rexaps.rexmanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rexaps.rexmanager.CollisionPolicy
import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.archive.ArchiveFormat
import com.rexaps.rexmanager.filesystem.FileMetadata
import com.rexaps.rexmanager.utils.*

@Composable
private fun NameField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    error: String?,
    label: String
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        singleLine = true, isError = error != null,
        supportingText = { error?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun RenameDialog(file: RexFile, existingNames: Set<String>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var field by remember {
        mutableStateOf(TextFieldValue(file.name, TextRange(0, FileNameUtils.baseName(file.name).length)))
    }
    val name = field.text.trim()
    val error = if (name == file.name) null else FileNameUtils.validate(name, existingNames - file.name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = { NameField(field, { field = it }, error, "Name") },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = error == null && name != file.name) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun NewFolderDialog(isFolder: Boolean, existingNames: Set<String>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue("")) }
    val name = field.text.trim()
    val error = if (name.isEmpty()) null else FileNameUtils.validate(name, existingNames)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFolder) "New folder" else "New file") },
        text = { NameField(field, { field = it }, error, "Name") },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotEmpty() && error == null) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteDialog(files: List<RexFile>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete permanently?") },
        text = {
            Text(
                if (files.size == 1) "\"${files[0].name}\" will be deleted and can't be recovered."
                else "${files.size} items will be deleted and can't be recovered."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun <T> RadioGroup(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Column {
        options.forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth()
                    .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(label)
            }
        }
    }
}

@Composable
fun CompressDialog(
    files: List<RexFile>,
    destinationPath: String,
    existingNames: Set<String>,
    onConfirm: (String, ArchiveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember {
        mutableStateOf(TextFieldValue(if (files.size == 1) FileNameUtils.baseName(files[0].name) else "Archive"))
    }
    var format by remember { mutableStateOf(ArchiveFormat.ZIP) }
    val name = field.text.trim()
    val error = FileNameUtils.validate(name, emptySet()).takeIf { name.isNotEmpty() }
        ?: if ("$name.${format.extension}" in existingNames) "An item with this name already exists." else null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress ${files.size} item${if (files.size == 1) "" else "s"}") },
        text = {
            Column {
                NameField(field, { field = it }, error, "Archive name")
                Spacer(Modifier.height(8.dp))
                RadioGroup(ArchiveFormat.values().map { "${it.label} — ${it.description}" to it }, format) { format = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, format) }, enabled = name.isNotEmpty() && error == null) { Text("Compress") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExtractDialog(
    archive: RexFile,
    parentPath: String,
    onConfirm: (String, CollisionPolicy) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember { mutableStateOf(TextFieldValue(FileNameUtils.baseName(archive.name))) }
    var policy by remember { mutableStateOf(CollisionPolicy.RENAME) }
    val name = field.text.trim()
    val error = FileNameUtils.validate(name, emptySet()).takeIf { name.isNotEmpty() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extract ${archive.name}") },
        text = {
            Column {
                Text("Into: $parentPath", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                NameField(field, { field = it }, error, "Folder name")
                Text("If a file already exists:", style = MaterialTheme.typography.labelLarge)
                RadioGroup(
                    listOf("Keep both" to CollisionPolicy.RENAME, "Replace" to CollisionPolicy.REPLACE, "Skip" to CollisionPolicy.SKIP),
                    policy
                ) { policy = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, policy) }, enabled = name.isNotEmpty() && error == null) { Text("Extract") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PropertiesDialog(metadata: FileMetadata, totalSize: Long?, onDismiss: () -> Unit) {
    val sizeText = when {
        totalSize == null -> "Calculating…"
        totalSize < 0 -> "-"
        else -> FileSizeFormatter.format(totalSize)
    }
    val perms = buildString {
        append(if (metadata.canRead) "R" else "-")
        append(if (metadata.canWrite) "W" else "-")
        append(if (metadata.canExecute) "X" else "-")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(metadata.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PropRow("Type", FileTypeResolver.typeLabel(metadata.isDirectory, metadata.extension, metadata.mimeType))
                PropRow("Location", metadata.parentPath)
                PropRow("Size", sizeText)
                metadata.itemCount?.let { PropRow("Items", it.toString()) }
                PropRow("Modified", DateFormatter.format(metadata.lastModified))
                PropRow("Permissions", perms)
                if (metadata.isSymlink) PropRow("Link", "Symbolic link")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun PropRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun HideFileDialog(files: List<RexFile>, hide: Boolean, conflicts: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val verb = if (hide) "Hide" else "Unhide"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$verb ${files.size} item${if (files.size == 1) "" else "s"}?") },
        text = {
            Column {
                Text(if (hide) "A dot will be added to the start of the name." else "The leading dot will be removed from the name.")
                if (conflicts > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("$conflicts name conflict(s): those items will get a number added.",
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(verb) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CollisionDialog(conflicts: List<String>, isMove: Boolean, onResolve: (CollisionPolicy) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name conflict") },
        text = {
            val shown = conflicts.take(5).joinToString("\n") { "• $it" }
            val more = if (conflicts.size > 5) "\n…and ${conflicts.size - 5} more" else ""
            Text("Already exists in the destination:\n$shown$more")
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onResolve(CollisionPolicy.RENAME) }) { Text("Keep both") }
                TextButton(onClick = { onResolve(CollisionPolicy.SKIP) }) { Text("Skip") }
                TextButton(onClick = { onResolve(CollisionPolicy.REPLACE) }) { Text("Replace") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
