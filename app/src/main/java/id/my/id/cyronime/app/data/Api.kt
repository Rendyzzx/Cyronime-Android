package id.my.id.cyronime.app.data

import android.content.Context
import android.webkit.CookieManager
import id.my.id.cyronime.app.BuildConfig
import id.my.id.cyronime.app.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error HTTP dari backend (4xx/5xx) — berbeda dari IOException (offline/timeout). */
class HttpError(val code: Int, message: String) : Exception(message)

/**
 * Klien API Cyronime — SEMUA data lewat backend (tidak pernah menyentuh API
 * sumber langsung). Session Auth.js berupa cookie httpOnly di CookieManager
 * WebView; OkHttp menjembataninya lewat WebViewCookieJar.
 */
object Api {

    val base: String = BuildConfig.WEB_URL.trimEnd('/')
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .cookieJar(WebViewCookieJar())
        .build()

    /* ---------- session ---------- */

    fun hasSession(): Boolean {
        val cookies = CookieManager.getInstance().getCookie(base) ?: return false
        return cookies.contains("session-token")  // cocok juga utk "__Secure-authjs.session-token"
    }

    fun clearSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    /**
     * Login NATIVE: tukar Google ID Token dengan session Auth.js lewat
     * provider "google-idtoken" di backend. Cookie session yang di-set
     * server tersimpan otomatis di CookieManager (WebViewCookieJar),
     * jadi dipakai bersama OkHttp maupun WebView embed video.
     */
    suspend fun nativeLogin(idToken: String): Me {
        val ua = "Cyronime-Android/" + BuildConfig.VERSION_NAME
        withContext(Dispatchers.IO) {
            // 1) ambil CSRF token Auth.js
            val csrfReq = Request.Builder().url(base + "/api/auth/csrf").header("User-Agent", ua).build()
            val csrf = client.newCall(csrfReq).execute().use { res ->
                if (!res.isSuccessful) throw HttpError(res.code, "HTTP ${res.code}")
                JSONObject(res.body?.string() ?: "{}").optString("csrfToken")
            }
            if (csrf.isBlank()) throw HttpError(0, "CSRF tidak tersedia")

            // 2) POST callback credentials (form-urlencoded, sesuai Auth.js)
            val form = FormBody.Builder()
                .add("csrfToken", csrf)
                .add("idToken", idToken)
                .add("callbackUrl", "/")
                .add("json", "true")
                .build()
            val loginReq = Request.Builder().url(base + "/api/auth/callback/google-idtoken")
                .header("User-Agent", ua).post(form).build()
            client.newCall(loginReq).execute().use { res ->
                val finalUrl = res.request.url.toString()
                if (finalUrl.contains("error=")) {
                    val code = res.request.url.queryParameter("error") ?: "unknown"
                    throw HttpError(401, "Ditolak server ($code)")
                }
                if (!res.isSuccessful) throw HttpError(res.code, "HTTP ${res.code}")
                // Bukti: apakah server benar-benar mengirim cookie sesi?
                val setCookies = res.headers("Set-Cookie")
                val gotSession = setCookies.any { it.contains("session-token") }
                if (!gotSession) {
                    throw HttpError(0, "Server tidak mengirim cookie sesi (HTTP ${res.code}, " +
                        "${setCookies.size} cookie)")
                }
            }
        }
        // 3) cookie harus benar-benar tersimpan di CookieManager
        if (!hasSession()) throw HttpError(0, "Cookie sesi gagal disimpan di perangkat")
        // 4) verifikasi session: /api/me harus mengenali user
        return try {
            me() ?: throw HttpError(0, "Session tidak terbentuk")
        } catch (e: HttpError) {
            throw HttpError(e.code, "Sesi ditolak server saat verifikasi (HTTP ${e.code})")
        }
    }

    /* ---------- HTTP core ---------- */

    private suspend fun execute(method: String, path: String, body: String? = null): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(base + path)
                .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post((body ?: "{}").toRequestBody(JSON_MEDIA))
                "PUT" -> builder.put((body ?: "{}").toRequestBody(JSON_MEDIA))
                "PATCH" -> builder.patch((body ?: "{}").toRequestBody(JSON_MEDIA))
                "DELETE" -> builder.delete()
                else -> builder.get()
            }
            client.newCall(builder.build()).execute().use { res ->
                val text = res.body?.string() ?: ""
                if (!res.isSuccessful) {
                    throw HttpError(res.code, "HTTP ${res.code}")
                }
                text
            }
        }

    /**
     * Cache memori singkat khusus data publik (list/detail anime): kembali dari
     * halaman detail / pindah tab terasa instan & tidak menembak ulang server.
     * Data user (progress, favorit, history) TIDAK pernah lewat cache ini.
     */
    private class Cached(val at: Long, val body: JSONObject)
    private val publicCache = object : java.util.LinkedHashMap<String, Cached>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>?) = size > 40
    }
    private const val PUBLIC_TTL_MS = 120_000L

    private suspend fun getPublicJson(path: String): JSONObject {
        val now = System.currentTimeMillis()
        synchronized(publicCache) {
            publicCache[path]?.let { if (now - it.at < PUBLIC_TTL_MS) return it.body }
        }
        val fresh = getJson(path)
        synchronized(publicCache) { publicCache[path] = Cached(now, fresh) }
        return fresh
    }

    private suspend fun getJson(path: String): JSONObject {
        val text = execute("GET", path)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private suspend fun getArray(path: String): JSONArray {
        val text = execute("GET", path)
        return if (text.isBlank()) JSONArray() else JSONArray(text)
    }

    /* ---------- system ---------- */

    suspend fun systemStatus(): SystemStatus =
        SystemStatus.parse(getJson("/api/system/status?platform=android"))

    suspend fun appVersion(): AppVersion = AppVersion.parse(getJson("/api/app/version"))

    suspend fun me(): Me? {
        val text = execute("GET", "/api/me")
        return if (text.isBlank()) null else Me.parse(JSONObject(text))
    }

    /* ---------- device & preferensi notifikasi ---------- */

    suspend fun registerDevice(ctx: Context, token: String) {
        val body = JSONObject()
            .put("platform", "android")
            .put("token", token)
            .put("deviceId", Prefs.deviceId(ctx))
        execute("POST", "/api/devices", body.toString())
    }

    suspend fun touchDevice(ctx: Context) {
        val body = JSONObject().put("deviceId", Prefs.deviceId(ctx))
        execute("PATCH", "/api/devices", body.toString())
    }

    suspend fun unregisterDevice(ctx: Context) {
        execute("DELETE", "/api/devices?deviceId=" + Prefs.deviceId(ctx))
    }

    suspend fun getNotifyPrefs(): NotifyPrefs {
        val text = execute("GET", "/api/notifications/prefs")
        val o = if (text.isBlank()) JSONObject() else JSONObject(text)
        return NotifyPrefs.parse(o.optJSONObject("prefs") ?: JSONObject())
    }

    suspend fun setNotifyPref(key: String, value: Boolean) {
        val body = JSONObject().put("prefs", JSONObject().put(key, value))
        execute("PUT", "/api/notifications/prefs", body.toString())
    }

    /* ---------- katalog ---------- */

    data class ListPage<T>(val items: List<T>, val hasNextPage: Boolean)

    suspend fun animeList(tab: String, page: Int): ListPage<AnimeItem> {
        val o = getPublicJson("/api/anime/list?tab=$tab&page=$page")
        val arr = o.optJSONArray("items") ?: JSONArray()
        return ListPage(
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(AnimeItem::parse) },
            o.optBoolean("hasNextPage", false)
        )
    }

    suspend fun donghuaList(tab: String, page: Int): ListPage<DonghuaItem> {
        val o = getPublicJson("/api/donghua/list?tab=$tab&page=$page")
        val arr = o.optJSONArray("items") ?: JSONArray()
        return ListPage(
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(DonghuaItem::parse) },
            o.optBoolean("hasNextPage", false)
        )
    }

    suspend fun search(q: String): SearchResults {
        val o = getJson("/api/search?q=" + java.net.URLEncoder.encode(q, "UTF-8"))
        val a = o.optJSONArray("anime") ?: JSONArray()
        val d = o.optJSONArray("donghua") ?: JSONArray()
        return SearchResults(
            (0 until a.length()).mapNotNull { i ->
                a.optJSONObject(i)?.let { it.optString("title") to it.optString("animeId") }
            },
            (0 until d.length()).mapNotNull { i ->
                d.optJSONObject(i)?.let { it.optString("title") to it.optString("slug") }
            }
        )
    }

    suspend fun animeDetail(slug: String): AnimeDetail {
        val o = getPublicJson("/api/anime/" + java.net.URLEncoder.encode(slug, "UTF-8"))
        return AnimeDetail.parse(o.optJSONObject("detail") ?: throw HttpError(404, "not found"))
    }

    suspend fun donghuaDetail(slug: String): DonghuaDetail {
        val o = getPublicJson("/api/donghua/" + java.net.URLEncoder.encode(slug, "UTF-8"))
        return DonghuaDetail.parse(o.optJSONObject("detail") ?: throw HttpError(404, "not found"))
    }

    suspend fun animeEpisode(episodeId: String): AnimeEpisode {
        val o = getJson("/api/anime/episode/" + java.net.URLEncoder.encode(episodeId, "UTF-8"))
        return AnimeEpisode.parse(o.optJSONObject("episode") ?: throw HttpError(404, "not found"))
    }

    suspend fun donghuaEpisode(slug: String): DonghuaEpisode {
        val o = getJson("/api/donghua/episode/" + java.net.URLEncoder.encode(slug, "UTF-8"))
        return DonghuaEpisode.parse(o.optJSONObject("episode") ?: throw HttpError(404, "not found"))
    }

    /** Resolve serverId anime -> URL embed/hls (cached di backend). */
    suspend fun resolveServer(serverId: String): String {
        val o = getJson("/api/anime/server/" + java.net.URLEncoder.encode(serverId, "UTF-8"))
        return o.optString("url")
    }

    /* ---------- aktivitas user ---------- */

    suspend fun progressList(): List<WatchProgress> {
        val o = getJson("/api/watch/progress")
        val arr = o.optJSONArray("progress") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let(WatchProgress::parse)
        }
    }

    suspend fun postProgress(
        contentId: String,
        type: String,
        episodeId: String,
        episode: Int?,
        title: String,
        poster: String
    ) {
        val body = JSONObject()
            .put("contentId", contentId)
            .put("type", type)
            .put("episodeId", episodeId)
            .put("episode", if (episode == null) JSONObject.NULL else episode)
            .put("title", title)
            .put("poster", poster)
        execute("POST", "/api/watch/progress", body.toString())
    }

    suspend fun history(type: String? = null): List<HistoryEntry> {
        val path = if (type == null) "/api/history" else "/api/history?type=$type"
        val o = getJson(path)
        val arr = o.optJSONArray("history") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let(HistoryEntry::parse)
        }
    }

    suspend fun favorites(type: String): List<Favorite> {
        val o = getJson("/api/favorites?type=$type")
        val arr = o.optJSONArray("favorites") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let(Favorite::parse)
        }
    }

    suspend fun addFavorite(type: String, contentId: String, title: String, poster: String) {
        val body = JSONObject()
            .put("type", type)
            .put("contentId", contentId)
            .put("title", title)
            .put("poster", poster)
        execute("POST", "/api/favorites", body.toString())
    }

    suspend fun removeFavorite(type: String, contentId: String) {
        execute("DELETE", "/api/favorites?type=$type&contentId=" + java.net.URLEncoder.encode(contentId, "UTF-8"))
    }

    suspend fun logout() {
        // signout Auth.js (GET mengikuti redirect ke "/"), lalu bersihkan cookie.
        try {
            execute("GET", "/api/auth/signout")
        } catch (err: IOException) {
            throw err
        } catch (_: Exception) {
            // 403/CSRF dsb: lanjut bersih-bersih cookie lokal.
        }
    }
}

/**
 * Jembatan cookie: WebView login menulis cookie Auth.js (httpOnly) ke
 * CookieManager platform; OkHttp membacanya di sini dan menulis cookie baru
 * yang di-set server saat request API.
 */
class WebViewCookieJar : CookieJar {

    private val cm: CookieManager get() = CookieManager.getInstance()

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = cm.getCookie(url.toString()) ?: return emptyList()
        return raw.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (c in cookies) {
            try {
                // Pertahankan SEMUA atribut. Cookie Auth.js "__Secure-authjs.session-token"
                // WAJIB Secure + Path=/; tanpa itu CookieManager menolaknya dan sesi tidak
                // pernah terbentuk (login tampak berhasil tapi langsung kembali ke awal).
                val sb = StringBuilder()
                sb.append(c.name).append('=').append(c.value)
                sb.append("; Path=").append(if (c.path.isNotEmpty()) c.path else "/")
                if (c.persistent) {
                    sb.append("; Expires=").append(
                        java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", java.util.Locale.US)
                            .apply { timeZone = java.util.TimeZone.getTimeZone("GMT") }
                            .format(java.util.Date(c.expiresAt))
                    )
                }
                if (c.secure) sb.append("; Secure")
                if (c.httpOnly) sb.append("; HttpOnly")
                sb.append("; SameSite=Lax")
                // Host-only cookie: JANGAN set Domain. Cookie domain-wide: set Domain.
                if (!c.hostOnly) sb.append("; Domain=").append(c.domain)
                cm.setCookie(url.toString(), sb.toString())
            } catch (_: Exception) {
                // abaikan
            }
        }
        cm.flush()
    }
}
