# RexMusic Upgrade — Hybrid Search & Manual Add

## Perubahan pada revisi ini

- Tombol tambah musik dipindah dari samping kolom pencarian ke header tab **Cari**. Ikonnya lebih kecil dan memakai warna sekunder agar tidak mendominasi UI.
- Sheet **Tambah musik** disederhanakan menjadi empat isian saja: `Nama Music`, `Nama Penyanyi`, `Link Music/File`, dan `Source dari`. Tidak ada tab provider, pilihan tema, kolom pencarian katalog, atau panel lain.
- Sumber pada form: Spotify, YouTube, Apple Music, dan Lokal. Ikon lampiran pada kolom Link Music/File membuka pemilih file MP3. Isian nama dan penyanyi yang diberikan pengguna dipakai sebagai metadata impor dan mengesampingkan tag ID3 bila diisi.
- Form menerima tautan Spotify/YouTube/Apple Music yang valid. Tautan Spotify track memakai resolver Spotify RexMusic; tautan YouTube dan Apple Music dibuka di layanan resminya.
- Kolom pencarian utama kini menjalankan **Hybrid Search** otomatis ke Spotify (API search RexMusic yang sudah ada), YouTube (`/search/youtube`), dan Apple Music (`/search/applemusic`) secara paralel. Hasil digabung dalam satu daftar dan setiap kartu tetap menampilkan badge sumber.
- Kegagalan satu provider tidak menghilangkan hasil dari dua provider lain. Hanya jika ketiganya gagal, pencarian menampilkan pesan error.
- Klik hasil Spotify memutar lewat resolver RexMusic. Klik hasil YouTube/Apple Music membuka halaman layanan terkait; hasil katalog eksternal tidak ditawarkan sebagai unduhan atau antrian audio langsung.

## Metadata offline dan batas layanan

- Impor MP3 tetap disimpan ke koleksi offline. Metadata judul, artis, album, durasi, dan artwork dibaca dari file bila tersedia; judul/artis dari form menjadi prioritas bila diisi.
- Metadata offline mempertahankan `source`, `sourceUrl`, dan `variant`, sehingga badge sumber/tema tetap tersedia di kartu lagu dan data terkait.
- Modul ini tidak mengekstrak audio dari YouTube atau Apple Music. Untuk menyimpan lagu offline, impor file audio yang kamu miliki atau punya izin untuk menyimpannya.

## Tema dan catatan build

- `RexMusicTheme.kt` tidak diubah. Engine tema tetap seperti sebelumnya.
- Arsip sumber yang diberikan tidak menyertakan `settings.gradle`, `build.gradle`, Android manifest, atau Gradle wrapper. File katalog hybrid berhasil diperiksa dengan compiler Kotlin menggunakan stub dependensi; kompilasi Android penuh tetap perlu dilakukan pada proyek host RexAps.

## Revisi lanjutan — thumbnail, offline, dan perbaikan compile

- Memperbaiki pemanggilan `ModeChip` di `RexMusicTabsNav2.kt` dengan argumen bernama `onToggle = ...`. Ini mengatasi error Kotlin ketika trailing lambda sebelumnya terikat ke parameter `Modifier` dan membuat `onToggle` dianggap belum diisi.
- Form Tambah Musik kini mempunyai field kelima `Pilih Thumbnail`, menerima foto dari pemilih gambar atau tautan langsung `.jpg/.jpeg`.
- Thumbnail pilihan disalin ke folder lagu offline sebagai `thumbnail.jpg`, `thumbnail.png`, atau `thumbnail.webp`. Jika thumbnail khusus gagal dibaca, artwork bawaan MP3 tetap menjadi fallback.
- Impor MP3 tetap langsung masuk ke `Download/RexAps/Music` dan memanggil refresh koleksi offline setelah selesai. Metadata sumber, judul, artis, dan thumbnail dibaca kembali saat aplikasi memindai koleksi.
- Tautan thumbnail diunduh hanya sebagai gambar (maksimum 8 MB), dengan timeout koneksi dan validasi tipe konten. Ini tidak mengunduh audio dari penyedia eksternal.
- UI form dirapikan untuk layar pendek: konten dapat di-scroll, memakai jarak/tinggi field konsisten, dan label status foto muncul ketika foto lokal dipilih.
- File `RexMusicTheme.kt` tetap tidak disentuh.

Catatan integrasi: source yang diserahkan hanya berupa modul `rexmusic/`, sehingga tidak ada fitur editor/pembuat video di dalam arsip ini untuk dihubungkan langsung. Audio MP3 yang diimpor sekarang otomatis masuk ke koleksi offline RexMusic; integrasi ke daftar pemilih audio pada fitur pembuat video memerlukan source modul pembuat video dari proyek host.
