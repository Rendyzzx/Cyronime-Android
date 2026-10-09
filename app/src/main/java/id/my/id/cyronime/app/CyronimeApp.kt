package id.my.id.cyronime.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.webkit.CookieManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Application — setup sekali: notification channel + cookie store WebView
 * (dipakai bridge ke OkHttp untuk session Auth.js) + cache gambar global
 * (Coil) supaya poster tidak diunduh ulang saat pindah halaman / scroll.
 */
class CyronimeApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(180)
            // Memory cache 20% RAM — cukup untuk thumbnail daftar panjang.
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            // Disk cache poster & backdrop; HTTPS disajikan ulang dari cache.
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()

    override fun onCreate() {
        super.onCreate()

        id.my.id.cyronime.app.data.Api.initCache(this)

        // Cookie WebView: aktifkan + persist agar session bertahan restart app.
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)

        // Channel notifikasi (sama dengan channel_id yang dikirim backend FCM).
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                "Cyronime",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Episode baru, pengumuman, maintenance, update aplikasi"
            }
        )
    }

    companion object {
        const val CHANNEL_GENERAL = "cyronime_general"
    }
}
