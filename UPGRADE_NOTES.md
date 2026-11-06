# RexAps Upgrade Notes

## RexGit

- Open a repository and select the **Release** tab.
- Tap **Draft a new release**, enter a tag (for example `v27.27`), title and release notes.
- Optionally attach an APK or another file. RexGit creates the GitHub Release first and then uploads the selected file as a release asset.
- The selected GitHub account token needs **Contents: write** permission for the repository. GitHub may also require the tag to be new.
- README Markdown image destinations now resolve relative to the checked-out repository branch. External HTTPS image links continue to work, and the existing app theme controls the preview colors.

## RexMusic Sync

- The room dashboard lists the playlists saved on the current device under **Playlist kamu**.
- Selecting **Share** places the playlist tracks in the room queue. Tracks not yet stored offline are downloaded on the sending device first, then their audio and available metadata are sent over the room's local Wi-Fi transfer channel.
- The host relays received audio to other members. A late-joining member also receives the currently playing track and the queued tracks.
- Playlist sharing requires the RexMusic storage permission and internet access when a track has to be downloaded. Audio distribution between room members uses the same local Wi-Fi network.
- The room player supports repeat from **0 (off) through 20 repeats**. Members can request a repeat setting; the host applies and broadcasts the authoritative counter.
- Queue playback waits for an incoming track's cache to become available instead of failing after a short fixed delay. Transfer receive timeout has also been extended for larger audio files.

## Build

Use Android Studio with Android SDK 35 installed, or run `./gradlew :app:assembleDebug` from the project root. The source changes were reviewed statically in this environment, but a full Android Gradle build could not be run here because the Gradle 8.11.1 distribution and Android SDK are not installed in the execution environment.

## Upgrade lanjutan — Animation & launcher shortcuts

- Pengaturan Animation global sekarang lebih tegas: `OFF` menghilangkan stagger/entrance, ambient drift dan sweep, partikel/grid dekoratif di shell, auto-advance banner, avatar spin, serta scale feedback. Bayangan elemen utama ikut dikurangi. Animasi penting proses/data tidak disentuh.
- `LOW`, `MEDIUM`, dan `HIGH` memakai intensitas transisi dan efek yang berbeda; `MEDIUM` adalah level yang dipakai saat Animation dinyalakan kembali dari OFF.
- Settings memiliki **Tambah shortcut**. Pilih modul dari seluruh entri `RexAppRegistry` dengan `available = true`, edit nama, lalu pilih ikon manual (simbol + warna) atau gambar sendiri.
- Shortcut menggunakan Android pinned-shortcut API. Launcher akan meminta konfirmasi; beberapa launcher/ROM dapat menolak fitur pinned shortcut.
- Intent shortcut membawa module ID ke `MainActivity` dan membuka route modul terkait secara langsung. Semua 14 modul yang tersedia saat perubahan ini memiliki route yang valid.

