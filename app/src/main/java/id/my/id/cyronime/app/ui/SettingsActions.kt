package id.my.id.cyronime.app.ui

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import id.my.id.cyronime.app.BuildConfig
import id.my.id.cyronime.app.Prefs
import id.my.id.cyronime.app.data.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlin.coroutines.resume

/**
 * Logika teknis yang dulu tertanam di layar Settings, dipisah dari UI.
 *
 * Tidak ada yang dihapus: registrasi device, sinkron notifikasi, hapus
 * history/progress, dan logout tetap memakai endpoint & mekanisme yang sama.
 * Yang berubah hanya: semuanya tidak lagi diperlihatkan sebagai panel teknis.
 */
object SettingsActions {

    /**
     * Pastikan perangkat ini terdaftar di server (token FCM terbaru). Aman
     * dipanggil berulang; semua error ditelan karena ini pemulihan senyap
     * (jalur utama tetap onNewToken + MainActivity saat login).
     */
    suspend fun ensureDeviceRegistered(ctx: Context) {
        try {
            val token = Prefs.fcmToken(ctx) ?: awaitFcmToken() ?: return
            Prefs.setFcmToken(ctx, token)
            Api.registerDevice(ctx, token)
        } catch (_: Exception) {
        }
    }

    private suspend fun awaitFcmToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }

    /** DELETE history (semua) / progress (?all=1) — endpoint sama dengan web. */
    suspend fun clearWatchData(kind: Kind) {
        withContext(Dispatchers.IO) {
            val path = if (kind == Kind.History) "/api/history" else "/api/watch/progress?all=1"
            val req = Request.Builder()
                .url(Api.base + path)
                .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
                .delete()
                .build()
            Api.client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
            }
        }
    }

    enum class Kind { History, Progress }

    /**
     * Logout: lepas device dari server, akhiri sesi, bersihkan cookie.
     * TIDAK menghapus data akun di server (history/favorit tetap ada).
     */
    suspend fun logout(ctx: Context) {
        try { Api.unregisterDevice(ctx) } catch (_: Exception) {}
        try { Api.logout() } catch (_: Exception) {}
        Api.clearSession()
        Prefs.setSessionDone(ctx, false)
    }
}
