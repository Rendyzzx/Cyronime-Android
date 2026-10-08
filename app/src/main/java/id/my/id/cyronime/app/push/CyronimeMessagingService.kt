package id.my.id.cyronime.app.push

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import id.my.id.cyronime.app.CyronimeApp
import id.my.id.cyronime.app.MainActivity
import id.my.id.cyronime.app.Prefs
import id.my.id.cyronime.app.R
import id.my.id.cyronime.app.data.Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FCM service — register token ke backend setelah token tersedia, tampilkan
 * notification, dan buka deep link saat notification ditekan.
 * Authorization TIDAK bersandarkan payload: deep link hanya alamat; halaman
 * tetap menuntut session login di backend.
 */
class CyronimeMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Prefs.setFcmToken(applicationContext, token)
        registerInBackground(token)
    }

    private fun registerInBackground(token: String) {
        scope.launch {
            try {
                Api.registerDevice(applicationContext, token)
            } catch (_: Exception) {
                // Belum login / offline: token disimpan, diberi tahu belakangan
                // (MainActivity register ulang setelah login sukses).
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Cyronime"
        val body = message.notification?.body ?: data["body"] ?: ""
        val deepLink = data["deepLink"] ?: data["url"]

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(toAppScheme(deepLink))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            (data["animeId"] ?: title).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CyronimeApp.CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify((title + System.currentTimeMillis() / 60_000).hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission notifikasi belum diberikan — abaikan.
        }
    }

    /** https://kamael.vercel.app/anime/x -> cyronime://anime/x (pastikan buka app, bukan browser). */
    private fun toAppScheme(link: String?): String {
        if (link.isNullOrBlank()) return "cyronime://anime"
        return try {
            val uri = Uri.parse(link)
            if (uri.scheme == "cyronime") link
            else "cyronime://" + (uri.host ?: "") + uri.path
        } catch (_: Exception) {
            "cyronime://anime"
        }
    }
}
