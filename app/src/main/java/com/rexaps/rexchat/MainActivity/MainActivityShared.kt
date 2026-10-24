package com.rexaps.rexchat

import android.content.Context
import android.content.Intent
import android.net.Uri

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
