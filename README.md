# Cyronime for Android

Aplikasi Android **native** (Kotlin + Jetpack Compose) untuk [Cyronime](https://cyronime.web.id).
Konsumsi **backend dan API yang sama dengan Web** — tidak ada backend streaming kedua, tidak ada API sumber yang disentuh langsung.

Repo backend/Web: [Rendyzzx/Kamael](https://github.com/Rendyzzx/Kamael) — kontrak API lengkap ada di `ANDROID_SETUP.md` di repo tersebut.

## Arsitektur

```
                CYRONIME BACKEND (Next.js /api + Upstash Redis)
                       |                        |
                CYRONIME WEB               CYRONIME ANDROID (repo ini)
                (tidak berubah)      - API sama, session Google sama
                                      - ExoPlayer utk HLS/MP4 langsung
                                      - WebView utk embed pihak ketiga (vidhide dll.)
                                      - FCM push (episode baru, pengumuman, maintenance, update)
                                      - Maintenance & versi via API (bukan hardcode)
```

- **Auth**: login Google lewat WebView in-app → cookie session Auth.js (httpOnly) mendar di `CookieManager` platform → dijembatani ke OkHttp (`WebViewCookieJar`). Akun, history, favorit, progress **sama persis dengan Web**.
- **Player**: URL `.m3u8/.mp4/.webm` → ExoPlayer (Media3). Sumber embed → WebView. Server anime di-resolve lazy via `GET /api/anime/server/{serverId}` (cache di backend).
- **Notification**: FCM token diregistrasi ke `POST /api/devices`; preferensi notifikasi disimpan di backend (`PUT /api/notifications/prefs`) sehingga toggle di Web dan Android selalu sinkron.
- **Maintenance**: `GET /api/system/status?platform=android` dicek saat startup dan tiap kembali ke foreground. Android-only maintenance tidak mengganggu Web, dan sebaliknya.
- **Update system**: `GET /api/app/version` — di bawah `minimumVersion`/`forceUpdate` = dialog wajib update; di bawah `latestVersion` = dialog opsional.

## Setup

### 1. Firebase (wajib untuk FCM)

1. [Firebase Console](https://console.firebase.google.com/) → buat project (atau pakai project `cyronime` yang dipakai backend).
2. Tambahkan aplikasi Android dengan **package name**: `id.my.id.cyronime.app`.
3. Unduh `google-services.json` → ganti file placeholder di `app/google-services.json`.

> File di repo adalah **placeholder** supaya build jalan tanpa Firebase; ganti dengan file asli agar push notification berfungsi. SHA-1 tidak wajib untuk FCM.

Di sisi backend (repo Kamael) pastikan env FCM HTTP v1 sudah diisi (`FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY`) — detail di `docs/ADMIN-TELEGRAM.md`.

### 2. Build

```bash
./gradlew assembleDebug        # APK debug
./gradlew assembleRelease       # AAB/APK release (isi signingConfig sendiri)
```

ataau buka folder ini di Android Studio (Hedgehog+) → Sync → Run.

**Kebutuhan:** JDK 17, Android SDK 34. Gradle 8.7 via wrapper (unduh otomatis saat pertama dijalankan).

### 3. Konfigurasi

- URL backend: `WEB_URL` di `app/build.gradle` (default `https://cyronime.web.id`).
- `versionCode`/`versionName` di `app/build.gradle` harus naik tiap rilis — `versionName` dibandingkan dengan `APP_LATEST_VERSION`/`APP_MINIMUM_VERSION` di backend.

## Struktur

```
app/src/main/java/id/my/id/cyronime/app/
├── CyronimeApp.kt        # Application: notification channel + cookie store
├── MainActivity.kt       # Nav + deep link + maintenance/force-update gate
├── Prefs.kt              # deviceId, token FCM
├── data/
│   ├── Api.kt            # OkHttp + bridge cookie session + semua endpoint
│   └── Models.kt         # model + parser JSON
├── push/
│   └── CyronimeMessagingService.kt  # FCM: register token + tampil notification
└── ui/
    ├── Theme.kt          # navy senada Web
    ├── Common.kt         # state umum (loading/error/maintenance), kartu, versi
    ├── LoginScreen.kt    # WebView Google OAuth + deteksi cookie session
    ├── HomeScreen.kt     # Continue Watching + Anime Ongoing + Donghua
    ├── SearchScreen.kt   # /api/search (backend proxy)
    ├── DetailScreen.kt   # detail + daftar episode + favorit
    ├── WatchScreen.kt    # ExoPlayer/WebView + pilih server + progress
    ├── LibraryScreen.kt  # History + Favorit
    └── SettingsScreen.kt # profil, preferensi notifikasi, logout
```

## Deep link

- `cyronime://anime/{slug}`, `cyronime://donghua/{slug}`, `cyronime://anime/watch/{episodeId}` — dipakai payload FCM.
- `https://cyronime.web.id/anime/...` (App Links, `autoVerify`) — aktif setelah `assetlinks.json` di-host di domain (lihat bagian App Links di `ANDROID_SETUP.md` repo Kamael).

## Catatan jujur (keterbatasan v1)

- Google terkadang menampilkan peringatan "browser tidak aman" saat OAuth di WebView. Jika itu terjadi, selesaikan login via browser lalu buka lagi app (cookie tidak dibagikan antar browser, jadi untuk v2 direncanakan token-based auth). Untuk sekarang, login di WebView umumnya berfungsi normal.
- Player embed tidak mengekspos posisi video (batasan sumber pihak ketiga) — progress yang dicatat adalah episode terakhir, sama seperti perilaku Web.
- Tidak ada register manual di app karena sistem existing hanya Google OAuth.
