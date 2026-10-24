// RexLocalAccountScreen.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun RexProfileController.__split_saveProfile_8(n: String, u: String, b: String) {
        val a = acct.copy(
            name = n.trim().ifBlank { "Rex User" },
            username = u.trim().ifBlank { "@rexuser" },
            bio = b.trim()
        )
        acct = a
        scope.launch(Dispatchers.IO) {
            try {
                writeAccount(a)
                withContext(Dispatchers.Main) { toast("Profil tersimpan") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }
