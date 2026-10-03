package com.rexaps.rexchat

import android.content.Context
import org.json.JSONObject
import java.io.File

object SessionManager {

    data class Session(
        val id: Int,
        val name: String,
        val created: Long,
        val lastOpened: Long = 0L,
        val linked: Boolean = false
    )

    private const val FILE = "session.json"
    private const val PENDING = "pending_delete"

    private fun root(ctx: Context) =
        File(ctx.applicationContext.filesDir, "sessions").apply { mkdirs() }

    private fun dir(ctx: Context, id: Int) = File(root(ctx), id.toString())

    fun suffix(id: Int) = "rexchat_session_$id"
    fun profileName(id: Int) = "rexchat_session_$id"

    // ---------- read ----------

    private fun read(ctx: Context, id: Int): Session? = runCatching {
        val f = File(dir(ctx, id), FILE)
        if (!f.exists()) return null
        val j = JSONObject(f.readText())
        Session(
            id = id,
            name = j.optString("name", "Session $id"),
            created = j.optLong("created", 0L),
            lastOpened = j.optLong("lastOpened", 0L),
            linked = j.optBoolean("linked", false)
        )
    }.getOrNull()

    fun get(ctx: Context, id: Int): Session? = read(ctx, id)

    fun list(ctx: Context): List<Session> =
        root(ctx).listFiles()
            ?.filter { it.isDirectory && !File(it, PENDING).exists() }
            ?.mapNotNull { it.name.toIntOrNull()?.let { id -> read(ctx, id) } }
            ?.sortedBy { it.id }
            ?: emptyList()

    // ---------- write ----------

    @Synchronized
    private fun write(ctx: Context, s: Session) {
        val d = dir(ctx, s.id).apply { mkdirs() }
        val json = JSONObject()
            .put("name", s.name)
            .put("created", s.created)
            .put("lastOpened", s.lastOpened)
            .put("linked", s.linked)
        val tmp = File(d, "$FILE.tmp")
        tmp.writeText(json.toString(2))
        tmp.renameTo(File(d, FILE))
    }

    @Synchronized
    fun create(ctx: Context, name: String?): Session {
        val r = root(ctx)
        val counterFile = File(r, "next_id")
        val counter = counterFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0
        val maxExisting = r.listFiles()?.mapNotNull { it.name.toIntOrNull() }?.maxOrNull() ?: 0
        val id = maxOf(counter, maxExisting) + 1
        counterFile.writeText(id.toString()) // ids are never reused

        val s = Session(
            id = id,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Session $id",
            created = System.currentTimeMillis()
        )
        write(ctx, s)
        return s
    }

    fun rename(ctx: Context, id: Int, newName: String) {
        val s = read(ctx, id) ?: return
        if (newName.isNotBlank()) write(ctx, s.copy(name = newName.trim()))
    }

    fun touch(ctx: Context, id: Int) {
        read(ctx, id)?.let { write(ctx, it.copy(lastOpened = System.currentTimeMillis())) }
    }

    fun markLinked(ctx: Context, id: Int, linked: Boolean = true) {
        read(ctx, id)?.let { if (it.linked != linked) write(ctx, it.copy(linked = linked)) }
    }

    // ---------- delete ----------

    /** Flags the session for deletion. Actual wipe happens in the main process via purgePending. */
    fun markPendingDelete(ctx: Context, id: Int) {
        runCatching { File(dir(ctx, id), PENDING).apply { parentFile?.mkdirs() }.writeText("1") }
    }

    fun delete(ctx: Context, id: Int) {
        markPendingDelete(ctx, id)
        purgePending(ctx)
    }

    /** Call from the MAIN process only (MainActivity.onResume). */
    @Synchronized
    fun purgePending(ctx: Context) {
        root(ctx).listFiles()?.forEach { d ->
            val id = d.name.toIntOrNull() ?: return@forEach
            if (File(d, PENDING).exists() && wipeWebViewData(ctx, id)) {
                d.deleteRecursively()
            }
        }
    }

    private fun wipeWebViewData(ctx: Context, id: Int): Boolean {
        var ok = true

        // Profile mode
        if (profilesSupported()) {
            ok = runCatching {
                val store = androidx.webkit.ProfileStore.getInstance()
                if (store.getProfile(profileName(id)) != null) {
                    store.deleteProfile(profileName(id))
                } else true
            }.getOrDefault(false)
        }

        // Legacy (data directory suffix) mode
        val sfx = suffix(id)
        runCatching {
            File(ctx.applicationInfo.dataDir).listFiles()
                ?.filter { it.name == "app_webview_$sfx" }
                ?.forEach { it.deleteRecursively() }
            ctx.cacheDir.listFiles()
                ?.filter { it.name.contains(sfx) }
                ?.forEach { it.deleteRecursively() }
        }
        return ok
    }

    fun profilesSupported(): Boolean = runCatching {
        androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)
    }.getOrDefault(false)
}