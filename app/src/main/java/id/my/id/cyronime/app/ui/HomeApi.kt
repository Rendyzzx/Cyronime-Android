package id.my.id.cyronime.app.ui

import id.my.id.cyronime.app.BuildConfig
import id.my.id.cyronime.app.data.AnimeItem
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.DonghuaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * Akses endpoint backend yang datanya SAMA dengan halaman web, tanpa
 * menyentuh data/Api.kt & data/Models.kt:
 * - tab=popular  -> ranking Terpopuler yang dipakai Home web (getPopularAnime)
 * - /api/search  -> item lengkap (poster/score/episodes) untuk grid hasil
 * Memakai Api.client (cookie session WebView yang sama).
 */
object HomeApi {

    data class SearchFull(
        val anime: List<AnimeItem>,
        val donghua: List<DonghuaItem>
    )

    /** GET /api/anime/list?tab=popular&page=N — sumber Terpopuler ala web. */
    suspend fun popularAnime(page: Int): List<AnimeItem> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(Api.base + "/api/anime/list?tab=popular&page=$page")
                .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
                .get()
                .build()
            Api.client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext emptyList()
                val o = JSONObject(res.body?.string() ?: "{}")
                val arr = o.optJSONArray("items") ?: return@withContext emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.let(AnimeItem::parse)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** GET /api/search?q=... — item lengkap untuk grid hasil pencarian. */
    suspend fun searchFull(q: String): SearchFull = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(Api.base + "/api/search?q=" + java.net.URLEncoder.encode(q, "UTF-8"))
                .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
                .get()
                .build()
            Api.client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext SearchFull(emptyList(), emptyList())
                val o = JSONObject(res.body?.string() ?: "{}")
                val a = o.optJSONArray("anime")
                val d = o.optJSONArray("donghua")
                SearchFull(
                    anime = if (a == null) emptyList() else
                        (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(AnimeItem::parse) },
                    donghua = if (d == null) emptyList() else
                        (0 until d.length()).mapNotNull { i -> d.optJSONObject(i)?.let(DonghuaItem::parse) }
                )
            }
        } catch (_: Exception) {
            SearchFull(emptyList(), emptyList())
        }
    }
}
