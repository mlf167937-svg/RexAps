package com.rexaps.ui

/**
 * Semua rute sub-aplikasi yang bisa dibuka dari Home.
 *
 * Menambah aplikasi baru ke navigasi cukup 2 langkah:
 *  1. Tambah entry di sini dengan `moduleId` yang sama persis dengan
 *     `id` milik modul tersebut di RexAppRegistry.
 *  2. Tangani satu case baru di blok `when (route)` pada RexApsApp.kt
 *     untuk memanggil layar composable-nya.
 *
 * Tidak perlu mengubah logic klik di HomeContent/onAppClick sama sekali —
 * itu semua sudah otomatis lewat fromModuleId().
 */
enum class RexRoute(
    val moduleId: String?,
    val displayName: String
) {
    NONE(null, ""),
    REXFOX("rexfox", "RexFox"),
    REXPANEL("rexpanel", "RexPanel"),
    REXCHAT("rexchat", "RexChat"),
    REXNUX("rexnux", "RexNux");

    companion object {
        fun fromModuleId(id: String): RexRoute? =
            entries.firstOrNull { it.moduleId == id }
    }
}
