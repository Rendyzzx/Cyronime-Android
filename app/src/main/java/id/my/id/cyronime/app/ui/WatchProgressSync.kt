package id.my.id.cyronime.app.ui

import id.my.id.cyronime.app.BuildConfig
import id.my.id.cyronime.app.data.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Sinkron posisi playback ke endpoint YANG SAMA dengan web
 * (POST/GET /api/watch/progress) — hanya menambahkan field opsional
 * position/duration yang juga dikirim player native web. Memakai
 * Api.client (cookie session WebView yang sama), tanpa menyentuh Api.kt.
 */
object WatchProgressSync {

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    data class ResumePoint(val episodeId: String, val position: Double, val duration: Double)

    /** GET /api/watch/progress?contentId=... -> posisi terakhir episode. */
    suspend fun fetchResume(contentId: String): ResumePoint? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(Api.base + "/api/watch/progress?contentId=" + java.net.URLEncoder.encode(contentId, "UTF-8"))
                .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
                .get()
                .build()
            Api.client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val o = JSONObject(res.body?.string() ?: "{}")
                val p = o.optJSONObject("progress") ?: return@withContext null
                val episodeId = p.optString("episodeId")
                val position = p.optDouble("position", 0.0)
                val duration = p.optDouble("duration", 0.0)
                if (episodeId.isBlank() || duration <= 0.0 || position <= 0.0) null
                else ResumePoint(episodeId, position, duration)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** POST /api/watch/progress dengan position/duration (interval 5 dtk ala web). */
    suspend fun save(
        contentId: String,
        type: String,
        episodeId: String,
        episode: Int?,
        title: String,
        poster: String,
        position: Long,
        duration: Long
    ) = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("contentId", contentId)
                .put("type", type)
                .put("episodeId", episodeId)
                .put("episode", if (episode == null) JSONObject.NULL else episode)
                .put("title", title)
                .put("poster", poster)
                .put("position", position)
                .put("duration", duration)
            val req = Request.Builder()
                .url(Api.base + "/api/watch/progress")
                .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            Api.client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
