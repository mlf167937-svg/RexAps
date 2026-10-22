# RexAps

<p align="center">
  <strong>Ruang kerja Android modular untuk produktivitas, coding, jaringan, dan hiburan.</strong>
  <br />
  Satu aplikasi dengan kumpulan modul yang terintegrasi dalam antarmuka dan sistem tema yang konsisten.
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img alt="Language" src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="UI" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" />
  <img alt="Design" src="https://img.shields.io/badge/Design-Material%203-6750A4" />
</p>

## Tentang proyek

**RexAps** adalah aplikasi Android berbasis Kotlin yang menyatukan berbagai utilitas dalam satu shell aplikasi. Setiap fungsi utama dipecah menjadi modul tersendiri—mulai dari browser dan editor kode sampai Git, terminal Linux, pemutar musik, dan pemantauan perangkat.

Antarmuka dibangun menggunakan Jetpack Compose dengan dukungan tampilan portrait dan landscape, navigasi antarmodul, serta sistem tema bersama.

## Daftar isi

- [Fitur utama](#fitur-utama)
- [Modul aplikasi](#modul-aplikasi)
- [Modul yang belum aktif](#modul-yang-belum-aktif)
- [Teknologi](#teknologi)
- [Arsitektur dan struktur proyek](#arsitektur-dan-struktur-proyek)
- [Menjalankan proyek](#menjalankan-proyek)
- [Catatan keamanan dan privasi](#catatan-keamanan-dan-privasi)
- [Kontribusi](#kontribusi)
- [Lisensi](#lisensi)

## Fitur utama

- **Desain modular** — fitur dipisahkan agar struktur kode dan pengembangan lebih mudah dikelola.
- **Navigasi terpadu** — modul yang tersedia dibuka melalui dashboard RexAps.
- **Tema bersama** — sistem tema dan palet digunakan lintas layar; kode tema mencakup kelompok dark, light, neon, glass, elemental, dan cosmic.
- **Layout adaptif** — sejumlah layar memiliki tata letak khusus untuk portrait dan landscape.
- **Fondasi bersama (`librex`)** — komponen untuk penanganan arsip, deteksi bahasa/syntax code, dan pengelolaan unduhan.

## Modul aplikasi

Daftar berikut dirangkum dari registry dan source code pada arsip yang ditinjau. Ketersediaan fitur tertentu tetap dapat bergantung pada izin Android, koneksi internet, layanan eksternal, dan dukungan perangkat.

| Modul | Fungsi | Yang terlihat di source code |
| --- | --- | --- |
| **RexFox** | Browser | Tab browser, navigasi alamat/pencarian, riwayat, bookmark, pengaturan search engine, dan pengelolaan privasi. |
| **RexCoder** | Editor kode | Workspace dan file explorer, tab editor, syntax highlighting untuk berbagai bahasa, terminal, sesi SSH, serta pengelolaan runtime. |
| **RexGit** | Git dan repositori | Pengelolaan repositori, operasi Git, informasi commit, autentikasi, serta integrasi API GitHub. |
| **RexTools** | Utilitas web dan media | Pencarian/unduh gambar Pinterest, screenshot halaman web, preview media dari URL/JSON, lookup profil TikTok, dan pengunduhan media TikTok. |
| **RexPanel** | Panel server | Pemantauan/kontrol server melalui SSH, terminal jarak jauh, serta alur pengelolaan dan transfer file. |
| **RexChat** | Klien pairing WhatsApp | Pengelolaan sesi dan alur pairing; modul dideskripsikan di registry sebagai **pairing only**. |
| **RexManager** | File manager | Penjelajahan file lokal, operasi file, serta dukungan melihat media dan membuka arsip. |
| **RexMusic** | Pemutar musik | Pemutaran, antrean, playlist, riwayat, koleksi offline/unduhan, dan tampilan lirik. |
| **RexNux** | Terminal Linux | Alur pemasangan lingkungan Linux dengan opsi Alpine Linux dan Arch Linux, terminal, serta pengaturan lingkungan. |
| **RexWARP** | Jaringan dan WARP | Kontrol koneksi VPN/tunnel, informasi koneksi, pemantauan penggunaan jaringan, tes kecepatan, serta konfigurasi DNS/MTU pada engine yang mendukungnya. |
| **RexMonitor** | Pemantauan perangkat | Ringkasan CPU, GPU, RAM, penyimpanan, suhu, baterai, jaringan, informasi sistem, dan fitur optimasi yang bergantung pada izin/akses perangkat. |
| **RexSpaceX** | Game space | Peluncur game dan layar pengaturan terkait sesi bermain, termasuk kontrol profil sentuh. |

### Modul yang belum aktif

Registry saat ini menandai modul berikut sebagai tidak tersedia (`available = false`), sehingga belum dapat dibuka dari dashboard utama:

- **RexCalc** — kalkulator pintar
- **RexAlbum** — galeri
- **RexTube** — video dan subscriptions
- **RexTok** — video pendek dan feed
- **RexAI** — alat AI dan generator

## Teknologi

Teknologi yang tampak digunakan pada source code meliputi:

- **Kotlin** untuk implementasi aplikasi.
- **Jetpack Compose**, Compose Animation, Foundation, Material, dan Material 3 untuk UI.
- **AndroidX** untuk Activity, lifecycle/ViewModel, navigasi, penyimpanan preferensi, dan komponen platform lainnya.
- **Kotlin Coroutines dan Flow** untuk pekerjaan asinkron serta aliran state.
- **Room dan DataStore** untuk kebutuhan penyimpanan pada modul terkait.
- **AndroidX Media3** untuk fungsi media tertentu.
- **OkHttp** dan **Coil** untuk kebutuhan jaringan dan pemuatan gambar pada modul yang menggunakannya.
- **Android WebView** untuk fungsi browser/integrasi berbasis web.

Versi dependency dan minimum Android SDK tidak dicantumkan di README ini karena file konfigurasi build tidak tersedia pada arsip sumber yang ditinjau.

## Arsitektur dan struktur proyek

Secara garis besar, aplikasi menggunakan satu shell utama untuk navigasi dan tema, lalu membuka layar dari modul yang sesuai.

```text
MainActivity
└── SplashScreen
    └── RexApsApp
        ├── Dashboard & navigasi
        ├── Sistem tema bersama
        └── Route ke modul aplikasi
            ├── RexFox
            ├── RexCoder
            ├── RexGit
            ├── RexTools
            ├── RexPanel / RexChat
            ├── RexManager / RexMusic
            ├── RexNux / RexWARP
            └── RexMonitor / RexSpaceX
```

### Direktori penting

```text
.
├── MainActivity.kt       # Entry point utama
├── ui/                   # Shell aplikasi, navigasi, tema, dan komponen UI bersama
├── librex/               # API dan utilitas bersama
│   ├── rexarchive/       # Deteksi, pembuatan, listing, dan ekstraksi arsip
│   ├── rexcode/          # Registry bahasa, lexer, dan syntax highlighting
│   └── rexsmartmanager/  # Kontrak dan implementasi pengelolaan unduhan
├── rexfox/               # Browser
├── rexcoder/             # Editor kode dan terminal
├── rexgit/               # Git dan repositori
├── rextools/             # Kumpulan tools
├── rexpanel/             # Panel server / SSH
├── rexchat/              # Sesi pairing chat
├── rexmanager/           # File manager
├── rexmusic/             # Pemutar dan pustaka musik
├── rexnux/               # Lingkungan Linux dan terminal
├── rexwarp/              # VPN/WARP dan pemantauan jaringan
├── rexmonitor/           # Pemantauan perangkat
└── rexspacex/            # Game space
```

### Tentang `librex`

`librex` menyediakan komponen yang dapat digunakan bersama oleh modul lain:

- **`rexarchive`** — mendeteksi format serta melakukan listing/ekstraksi arsip; mendukung sejumlah format seperti ZIP, TAR, TAR.GZ, TAR.XZ, TAR.BZ2, GZIP, BZIP2, XZ, 7z, dan ekstraksi RAR dengan kemampuan berbeda per format.
- **`rexcode`** — registry bahasa dan tokenisasi/syntax highlighting untuk beragam bahasa pemrograman, format konfigurasi, dan teks.
- **`rexsmartmanager`** — kontrak pengelolaan unduhan bersama, termasuk status/progres serta aksi pause, resume, dan cancel.

## Menjalankan proyek

> **Catatan:** ZIP yang dijadikan referensi untuk README ini hanya berisi source Kotlin dan beberapa file Markdown. `gradlew`, file `settings.gradle`, konfigurasi `build.gradle`, `AndroidManifest.xml`, dan resource aplikasi tidak ada di dalam arsip tersebut. Karena itu, perintah build dan versi SDK belum bisa diverifikasi dari snapshot ini.

Jika repository GitHub lengkap menyertakan konfigurasi Android/Gradle, alur umum pengembangannya adalah:

1. Clone repository dan buka **folder root proyek** di Android Studio.
2. Pastikan JDK serta Android SDK yang diperlukan oleh konfigurasi proyek sudah tersedia.
3. Jalankan **Gradle Sync** dan selesaikan dependency yang belum tersedia.
4. Pilih konfigurasi aplikasi utama, hubungkan perangkat Android atau siapkan emulator, lalu jalankan aplikasi.

Jangan menambahkan versi Gradle, `minSdk`, `compileSdk`, atau perintah build tertentu ke dokumentasi sebelum nilainya cocok dengan file konfigurasi proyek yang sebenarnya.

## Catatan keamanan dan privasi

Beberapa modul melakukan operasi yang berdampak pada perangkat atau berkomunikasi dengan layanan lain. Sebelum digunakan:

- Periksa izin yang diminta, terutama untuk VPN, akses penyimpanan, notifikasi, unduhan, dan akses root jika tersedia.
- Gunakan hanya server, URL, endpoint, dan repositori yang dipercaya. Perilaku modul online dapat berubah jika layanan eksternal berubah atau tidak tersedia.
- Jangan commit token GitHub, kredensial SSH, password, kunci, atau data akun ke repository.
- Pahami bahwa RexWARP dapat mengubah jalur koneksi ketika tunnel/VPN aktif; periksa konfigurasi jaringan sebelum mengaktifkannya.
- Hormati hak cipta, ketentuan layanan, dan privasi pemilik konten ketika menggunakan fitur pencarian atau unduhan media.

Catatan ini bukan hasil audit keamanan. Tinjau kode dan izin secara mandiri sebelum memasang atau mendistribusikan aplikasi.

## Kontribusi

Kontribusi dan laporan bug dapat diajukan melalui GitHub Issues atau Pull Request. Untuk mempermudah peninjauan, sertakan:

1. Modul dan layar yang terdampak.
2. Langkah untuk mereproduksi masalah.
3. Perilaku yang diharapkan dan yang terjadi.
4. Versi Android/perangkat serta log yang relevan—hapus informasi sensitif sebelum membagikannya.

Untuk perubahan fitur, usahakan menjaga pemisahan antar-modul dan menggunakan komponen atau tema bersama bila sesuai.

## Lisensi

Lisensi belum dapat dipastikan dari arsip sumber yang ditinjau karena tidak ada file `LICENSE` di dalamnya. Sebelum repository dipublikasikan untuk digunakan ulang atau didistribusikan, tambahkan file lisensi yang memang dipilih oleh pemilik proyek.
