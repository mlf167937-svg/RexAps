package com.rexaps.rexchat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// =====================================================================
// OPEN SESSION (dipakai MainActivity dan route UI utama)
// =====================================================================

fun openRexChatSession(
    context: Context,
    session: SessionManager.Session,
    method: String = ""
) {
    val modern = SessionManager.profilesSupported()

    val intent = Intent(
        context,
        if (modern) WaActivity::class.java else WaLegacyActivity::class.java
    )
        .putExtra(WaActivity.EXTRA_SESSION_ID, session.id)
        .putExtra(WaActivity.EXTRA_METHOD, method)

    if (modern) {
        // Setiap session = task sendiri di Recents
        intent.data = Uri.parse("rexchat://session/${session.id}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    // Aman dipanggil dari Context non-Activity
    if (context !is android.app.Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}

// =====================================================================
// ACTIVITY (opsional, kalau mau dijalankan standalone)
// =====================================================================

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            com.rexaps.ui.RexTheme(option = com.rexaps.ui.RexThemeStore(this@MainActivity).load()) {
                SessionListScreen(
                    refreshTick = tick,
                    onOpen = { session, method ->
                        openRexChatSession(this, session, method)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SessionManager.purgePending(this)
        tick++
    }
}

// =====================================================================
// SESSION LIST SCREEN
// =====================================================================

@Composable
fun SessionListScreen(
    refreshTick: Int = 0,
    onOpen: (SessionManager.Session, String) -> Unit,
    onExit: (() -> Unit)? = null
) {
        return __splitImpl_SessionListScreen(refreshTick, onOpen, onExit)
    }

// =====================================================================
// SESSION CARD
// =====================================================================

@Composable
internal fun SessionCard(
    s: SessionManager.Session,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.id.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    s.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val status = if (s.linked) "Linked" else "Not linked"
                val last =
                    if (s.lastOpened > 0) {
                        " • " + DateUtils.getRelativeTimeSpanString(
                            s.lastOpened,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )
                    } else ""

                Text(
                    status + last,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (s.linked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, "Rename")
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}