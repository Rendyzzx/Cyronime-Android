package id.my.id.cyronime.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.webkit.CookieManager

/**
 * Application — setup sekali: notification channel + cookie store WebView
 * (dipakai bridge ke OkHttp untuk session Auth.js).
 */
class CyronimeApp : Application() {

    override fun onCreate() {
        super.onCreate()

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
