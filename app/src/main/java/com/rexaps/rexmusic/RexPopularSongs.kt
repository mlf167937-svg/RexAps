package com.rexaps.rexmusic

/**
 * DAFTAR LAGU POPULER BUATANMU SENDIRI.
 *
 * Cara isi: ganti song("", "") dengan song("Judul", "Artis"). Contoh:
 *     song("Jatuh Suka", "Tulus"),
 *
 * - Sudah tersedia 30 slot kosong. Mau lebih? Tambah baris song(...) baru.
 * - Baris yang judul ATAU artisnya kosong otomatis diabaikan.
 * - Saat menekan Next (autoplay pintar), lagu diacak dari daftar ini.
 * - Hanya judul + artis yang disimpan, tidak ada mp3. Audio dicari lewat API saat diputar.
 */
object RexPopularSongs {

    private val SONGS: List<Pair<String, String>> = listOf(
        song("", ""), // 01
        song("", ""), // 02
        song("", ""), // 03
        song("", ""), // 04
        song("", ""), // 05
        song("", ""), // 06
        song("", ""), // 07
        song("", ""), // 08
        song("", ""), // 09
        song("", ""), // 10
        song("", ""), // 11
        song("", ""), // 12
        song("", ""), // 13
        song("", ""), // 14
        song("", ""), // 15
        song("", ""), // 16
        song("", ""), // 17
        song("", ""), // 18
        song("", ""), // 19
        song("", ""), // 20
        song("", ""), // 21
        song("", ""), // 22
        song("", ""), // 23
        song("", ""), // 24
        song("", ""), // 25
        song("", ""), // 26
        song("", ""), // 27
        song("", ""), // 28
        song("", ""), // 29
        song("", "")  // 30
    )

    private fun song(title: String, artist: String) = title to artist

    /** Hanya entri yang terisi lengkap, tanpa duplikat. */
    val tracks: List<RexTrack> by lazy {
        SONGS
            .filter { it.first.isNotBlank() && it.second.isNotBlank() }
            .distinctBy { "${it.first.trim().lowercase()}|${it.second.trim().lowercase()}" }
            .map { (title, artist) ->
                RexTrack(
                    id = "pop-" + slug("$title $artist"),
                    title = title.trim(),
                    artist = artist.trim(),
                    album = "Populer"
                )
            }
    }

    private fun slug(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
