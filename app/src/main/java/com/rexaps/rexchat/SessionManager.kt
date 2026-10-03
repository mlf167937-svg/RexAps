package com.rexaps.rexchat

import android.content.Context
import android.os.Build
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mengelola daftar session RexChat.
 *
 * Folder metadata:
 *
 * /sdcard/Download/RexAps/RexChat/Session/1/
 * /sdcard/Download/RexAps/RexChat/Session/2/
 * /sdcard/Download/RexAps/RexChat/Session/3/
 *
 * CATATAN:
 *
 * Data aktif WebView TIDAK disimpan di folder ini.
 *
 * WebView menyimpan:
 * - Cookie
 * - IndexedDB
 * - LocalStorage
 * - Service Worker
 * - Chromium data
 *
 * pada data directory internal Android berdasarkan
 * WebView.setDataDirectorySuffix().
 */
object SessionManager {

    private const val ROOT_NAME = "RexAps"
    private const val CHAT_NAME = "RexChat"
    private const val SESSION_NAME = "Session"

    private const val FILE_NAME = "session.json"

    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_CREATED = "created"

    /**
     * Model session.
     */
    data class Session(
        val id: Int,
        val name: String,
        val created: Long
    )

    /**
     * Root folder.
     *
     * Android modern:
     *
     * /storage/emulated/0/Download
     */
    fun getRootDirectory(): File {

        val download =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

        return File(
            download,
            "$ROOT_NAME/$CHAT_NAME/$SESSION_NAME"
        )
    }

    /**
     * Folder sebuah session.
     */
    fun getSessionDirectory(
        id: Int
    ): File {

        return File(
            getRootDirectory(),
            id.toString()
        )
    }

    /**
     * File metadata session.
     */
    private fun getSessionFile(
        id: Int
    ): File {

        return File(
            getSessionDirectory(id),
            FILE_NAME
        )
    }

    /**
     * Membuat root directory.
     */
    private fun ensureRoot() {

        runCatching {

            getRootDirectory()
                .mkdirs()

        }
    }

    /**
     * Membuat session baru.
     */
    @Synchronized
    fun createSession(
        context: Context,
        name: String? = null
    ): Session {

        ensureRoot()

        val nextId =
            findNextId()

        val sessionName =
            if (
                name.isNullOrBlank()
            ) {
                "Session $nextId"
            } else {
                name.trim()
            }

        val session =
            Session(
                id = nextId,
                name = sessionName,
                created = System.currentTimeMillis()
            )

        saveSession(session)

        return session
    }

    /**
     * Cari ID kosong berikutnya.
     */
    private fun findNextId(): Int {

        ensureRoot()

        val root =
            getRootDirectory()

        var id = 1

        while (true) {

            val directory =
                File(
                    root,
                    id.toString()
                )

            if (!directory.exists()) {
                return id
            }

            id++
        }
    }

    /**
     * Simpan metadata.
     */
    @Synchronized
    fun saveSession(
        session: Session
    ) {

        ensureRoot()

        val directory =
            getSessionDirectory(
                session.id
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val json =
            JSONObject().apply {

                put(
                    KEY_ID,
                    session.id
                )

                put(
                    KEY_NAME,
                    session.name
                )

                put(
                    KEY_CREATED,
                    session.created
                )
            }

        runCatching {

            getSessionFile(
                session.id
            ).writeText(
                json.toString(4),
                Charsets.UTF_8
            )
        }
    }

    /**
     * Load semua session.
     */
    @Synchronized
    fun getSessions(
        context: Context
    ): List<Session> {

        ensureRoot()

        val root =
            getRootDirectory()

        if (!root.exists()) {
            return emptyList()
        }

        return root
            .listFiles()
            ?.filter {
                it.isDirectory
            }
            ?.mapNotNull {
                readSession(
                    it.name.toIntOrNull()
                )
            }
            ?.sortedBy {
                it.id
            }
            ?: emptyList()
    }

    /**
     * Load satu session.
     */
    @Synchronized
    fun getSession(
        context: Context,
        id: Int
    ): Session? {

        return readSession(id)
    }

    /**
     * Membaca metadata.
     */
    private fun readSession(
        id: Int?
    ): Session? {

        if (id == null) {
            return null
        }

        val file =
            getSessionFile(id)

        if (!file.exists()) {
            return null
        }

        return runCatching {

            val json =
                JSONObject(
                    file.readText(
                        Charsets.UTF_8
                    )
                )

            Session(
                id =
                    json.optInt(
                        KEY_ID,
                        id
                    ),

                name =
                    json.optString(
                        KEY_NAME,
                        "Session $id"
                    ),

                created =
                    json.optLong(
                        KEY_CREATED,
                        0L
                    )
            )

        }.getOrNull()
    }

    /**
     * Rename session.
     */
    @Synchronized
    fun renameSession(
        context: Context,
        id: Int,
        newName: String
    ): Boolean {

        val current =
            getSession(
                context,
                id
            )
                ?: return false

        val name =
            newName.trim()

        if (name.isBlank()) {
            return false
        }

        saveSession(
            current.copy(
                name = name
            )
        )

        return true
    }

    /**
     * Hapus metadata session.
     *
     * PENTING:
     *
     * Ini TIDAK menghapus WebView Chromium data.
     *
     * Data WebView akan tetap ada karena WebView
     * data directory berada di internal storage.
     *
     * Untuk benar-benar menghapus data WebView,
     * kita gunakan suffix dan process cleanup.
     *
     * Penghapusan data Chromium secara langsung
     * tidak disarankan.
     */
    @Synchronized
    fun deleteSession(
        context: Context,
        id: Int
    ): Boolean {

        val directory =
            getSessionDirectory(id)

        if (!directory.exists()) {
            return false
        }

        return runCatching {
            directory.deleteRecursively()
        }.getOrDefault(false)
    }

    /**
     * Pastikan folder session ada.
     */
    fun ensureSessionDirectory(
        id: Int
    ): File {

        val directory =
            getSessionDirectory(id)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }

    /**
     * Nama suffix WebView.
     *
     * HARUS unik antar session.
     */
    fun getWebViewDataSuffix(
        id: Int
    ): String {

        return "rexchat_session_$id"
    }
}
