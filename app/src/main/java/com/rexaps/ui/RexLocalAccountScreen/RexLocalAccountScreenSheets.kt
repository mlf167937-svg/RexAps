// RexLocalAccountScreen.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditProfileSheet(ctl: RexProfileController) {
    val acct = ctl.acct
    var n by remember { mutableStateOf(acct.name) }
    var u by remember { mutableStateOf(acct.username) }
    var b by remember { mutableStateOf(acct.bio) }

    ModalBottomSheet(onDismissRequest = { ctl.showEdit = false }) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text("Edit profil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Perbarui foto, banner, dan identitas kamu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MediaOption(Icons.Default.AddAPhoto, "Banner", "Foto • video 4 dtk • live", Modifier.weight(1f)) {
                    ctl.showEdit = false
                    ctl.onPickBanner()
                }
                MediaOption(Icons.Default.AccountCircle, "Foto profil", "Foto • video 4 dtk • live", Modifier.weight(1f)) {
                    ctl.showEdit = false
                    ctl.onPickAvatar()
                }
            }

            OutlinedTextField(
                value = n, onValueChange = { n = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = u, onValueChange = { u = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = b, onValueChange = { b = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (acct.bannerPath.isNotBlank() || acct.bannerVideo.isNotBlank()) {
                    TextButton(onClick = { ctl.clearBanner() }) { Text("Hapus banner") }
                }
                if (acct.picturePath.isNotBlank()) {
                    TextButton(onClick = { ctl.clearAvatar() }) { Text("Hapus foto profil") }
                }
            }

            Button(
                onClick = {
                    ctl.saveProfile(n, u, b)
                    ctl.showEdit = false
                },
                Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Simpan profil") }
        }
    }
}
