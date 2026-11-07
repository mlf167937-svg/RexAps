# Panduan Release RexAps (untuk pertama kali)

Panduan ini untuk membuat tiga paket Release yang terpisah: ARM64, ARM 32-bit,
dan Universal. Workflow GitHub Actions menghasilkan APK Release yang sudah
ditandatangani, lalu membungkus setiap APK ke ZIP.

## 1. Apa yang akan dihasilkan?

Setelah workflow berhasil, artifact GitHub Actions bernama
`RexAps-Release-Files` berisi:

- `RexAps-arm64-v8a.zip` — untuk perangkat Android ARM 64-bit.
- `RexAps-armeabi-v7a.zip` — untuk perangkat Android ARM 32-bit.
- `RexAps-Universal.zip` — satu APK yang memuat kedua ABI ARM di atas.
- `Actions.zip` — berisi `Build.log`, `NodeJS-Download.log`, dan `Full-Actions.log`.

Universal di proyek ini berarti gabungan dua ABI ARM, bukan x86 atau x86_64.

## 2. Siapkan kunci Release satu kali

Release harus memakai satu keystore yang disimpan aman dan dipakai lagi untuk
semua pembaruan aplikasi. Jangan membuat keystore baru untuk setiap build dan
jangan unggah keystore atau password ke repository.

Bila belum punya keystore, buat satu kali di komputer yang memiliki Java
`keytool`:

```bash
keytool -genkeypair -v \\
  -keystore rexaps-release.jks \\
  -alias rexaps-release \\
  -keyalg RSA -keysize 4096 -validity 10000
```

Ubah file keystore menjadi satu baris Base64 di Linux/macOS:

```bash
base64 -w 0 rexaps-release.jks
```

Di GitHub repository, buka **Settings → Secrets and variables → Actions** lalu
buat empat repository secrets berikut:

- `RELEASE_KEYSTORE_BASE64` — hasil Base64 keystore.
- `RELEASE_STORE_PASSWORD` — password keystore.
- `RELEASE_KEY_ALIAS` — contoh alias `rexaps-release` yang kamu pilih.
- `RELEASE_KEY_PASSWORD` — password untuk alias/key tersebut.

Jangan masukkan password asli ke file proyek, chat, atau commit Git.
Workflow sengaja berhenti jika salah satu secret belum tersedia atau tidak
cocok. Itu mencegah APK Release yang tampak siap dipakai tetapi tidak memiliki
tanda tangan rilis yang benar.

## 3. Jalankan build Release

1. Unggah/push project ini ke repository GitHub milikmu pada branch `main`.
2. Buka tab **Actions**.
3. Pilih workflow **Build RexAps Release**.
4. Klik **Run workflow** jika ingin menjalankannya secara manual.
5. Buka run terbaru dan tunggu sampai status workflow selesai.
6. Jika berhasil, unduh artifact **RexAps-Release-Files**.
7. Ekstrak artifact yang diunduh. Di dalamnya ada tiga ZIP APK dan `Actions.zip`.

Push baru ke branch `main` juga akan memicu build secara otomatis.

## 4. APK mana yang dipilih?

- Ponsel ARM 64-bit: pilih `RexAps-arm64-v8a.zip`.
- Ponsel ARM 32-bit: pilih `RexAps-armeabi-v7a.zip`.
- Belum tahu ABI perangkat: `RexAps-Universal.zip` adalah pilihan yang mencakup
  kedua ABI ARM yang didukung proyek ini, tetapi ukuran unduhannya biasanya lebih
  besar daripada APK satu ABI.

Buka ZIP pilihan, ekstrak file `.apk`, lalu instal APK tersebut di perangkat.
Jangan mencoba memasang file ZIP langsung sebagai aplikasi.

## 5. Apa yang diverifikasi otomatis?

Workflow melakukan pemeriksaan berikut sebelum menandai build berhasil:

- `libnode.so` Node.js Mobile tersedia untuk ARM64 dan ARM 32-bit.
- Kelas dan tipe mesin ELF cocok dengan ABI foldernya.
- Gradle berhasil membangun ketiga APK Release.
- Tanda tangan setiap APK lolos `apksigner verify`.
- APK ARM64 hanya memuat `arm64-v8a`.
- APK ARM 32-bit hanya memuat `armeabi-v7a`.
- APK Universal memuat kedua ABI.
- Setiap ABI menyertakan `libnode.so` (Node.js Mobile) serta `libmozglue.so`
  dan `libxul.so` (inti native GeckoView).
- ZIP hasil paket lolos tes integritas.

## 6. Apakah langsung siap dirilis?

Jika workflow hijau dan ketiga ZIP tersedia, hasilnya sudah melewati pemeriksaan
build, tanda tangan, ABI, serta integritas ZIP. Namun itu belum membuktikan semua
fitur aplikasi berjalan di semua ponsel. Sebelum membagikan ke pengguna, lakukan
uji instalasi dan buka aplikasi pada setidaknya satu perangkat ARM64 dan satu
perangkat ARM 32-bit. Periksa fitur penting yang menggunakan GeckoView dan Node.js.

Jangan mengganti atau menghapus keystore lama. APK pembaruan harus memakai
sertifikat penandatanganan yang sama dengan versi yang sudah terpasang. Bila
kunci hilang atau berubah, Android dapat menolak pembaruan sebagai konflik
signature.

## 7. Jika build gagal

Unduh `Actions.zip` dari artifact, lalu periksa:

- `NodeJS-Download.log` untuk unduhan atau validasi pustaka Node.js.
- `Build.log` untuk error Gradle/Kotlin/Android.
- `Full-Actions.log` untuk ringkasan langkah dan hasil tiap tahap.

GitHub Actions juga menyimpan log lengkap setiap langkah di halaman run workflow.
Jika error terjadi sebelum tahap build, periksa empat repository secrets dahulu.
