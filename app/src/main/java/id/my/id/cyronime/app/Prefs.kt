package id.my.id.cyronime.app

import android.content.Context
import java.util.UUID

/**
 * Penyimpanan lokal minimal (deviceId, token FCM, flag onboarding app).
 * TIDAK menyimpan preference notifikasi — itu di backend (sinkron Web/Android).
 */
object Prefs {
    private const val FILE = "cyronime"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_SESSION_DONE = "session_done"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun deviceId(ctx: Context): String {
        val id = sp(ctx).getString(KEY_DEVICE_ID, null)
        if (id != null) return id
        val fresh = "android-" + UUID.randomUUID().toString()
        sp(ctx).edit().putString(KEY_DEVICE_ID, fresh).apply()
        return fresh
    }

    fun fcmToken(ctx: Context): String? = sp(ctx).getString(KEY_FCM_TOKEN, null)

    fun setFcmToken(ctx: Context, token: String) {
        sp(ctx).edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    /** Tanda user pernah menyelesaikan login di app (state nyata tetap cookie session). */
    fun sessionDone(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_SESSION_DONE, false)

    fun setSessionDone(ctx: Context, done: Boolean) {
        sp(ctx).edit().putBoolean(KEY_SESSION_DONE, done).apply()
    }
}
