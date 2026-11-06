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


## RexMusic Sync — drift correction and live profile media

- Playback start is prepared with a shared monotonic host deadline (5 seconds ahead). Each member preloads the local track and pre-seeks while waiting; it no longer starts by seeking at the exact play deadline.
- Every member sends a JSON playback/clock report every 2 seconds. The host broadcasts its own passive room snapshot at the same interval and compares each fresh member position against the host timeline.
- A room position spread of **1,000 ms or more** triggers one coordinated correction: the host captures a common target 1 second behind its current position, pauses/pre-seeks all ready devices, and schedules their restart on one shared deadline (4 seconds ahead). A 12-second cooldown prevents correction loops.
- The previous 5-minute automatic refresh is removed. There is no periodic forced seek/pause; **Refresh Sync · manual** remains available to the host. Normal 2-second telemetry updates the UI only and never pauses, seeks, or restarts a player.
- Members joining an active room receive a position projected to the scheduled start time, so the preparation window does not put them several seconds behind. A joining device waits briefly for a transferred audio cache to become indexed before attempting playback.
- Sync member banners use the full card width with a 16:9 crop. Volume controls are laid out below the banner/profile row so they cannot squeeze the banner.
- Local profile banner videos and profile-picture videos are transferred to room members (up to 6 MB each), cached locally, and played muted in a loop on the member cards. Static image fallbacks remain supported.

## Validation for this update

- Kotlin parser check reported no syntax/declaration parse errors in the RexMusic Sync source files. Standalone `kotlinc` checks cannot resolve Android/Compose project dependencies, so they are not a full compile.
- `./gradlew :app:compileDebugKotlin --offline` could not start compilation because the Gradle 8.11.1 wrapper distribution is not cached and this environment cannot resolve `services.gradle.org`. Run a full Android Studio/Gradle build on a machine with the required Gradle distribution and Android SDK before releasing an APK.
