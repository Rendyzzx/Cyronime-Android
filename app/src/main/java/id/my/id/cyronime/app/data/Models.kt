package id.my.id.cyronime.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Model + parser (org.json, tanpa library serialization tambahan).
 * Bentuk JSON persis dengan kontrak API backend (lihat ANDROID_SETUP.md repo Kamael).
 */

data class SystemStatus(
    val maintenance: Boolean,
    val message: String,
    val estimatedEnd: String?
) {
    companion object {
        fun parse(o: JSONObject) = SystemStatus(
            maintenance = o.optBoolean("maintenance", false),
            message = o.optString("message", ""),
            estimatedEnd = if (o.isNull("estimatedEnd")) null else o.optString("estimatedEnd")
        )
    }
}

data class AppVersion(
    val latestVersion: String,
    val minimumVersion: String,
    val downloadUrl: String?,
    val forceUpdate: Boolean
) {
    companion object {
        fun parse(o: JSONObject) = AppVersion(
            latestVersion = o.optString("latestVersion", "1.0.0"),
            minimumVersion = o.optString("minimumVersion", "1.0.0"),
            downloadUrl = if (o.isNull("downloadUrl")) null else o.optString("downloadUrl"),
            forceUpdate = o.optBoolean("forceUpdate", false)
        )
    }
}

data class Me(
    val id: String?,
    val name: String?,
    val email: String?,
    val image: String?
) {
    companion object {
        fun parse(o: JSONObject): Me? {
            val u = o.optJSONObject("user") ?: return null
            return Me(
                id = if (u.isNull("id")) null else u.optString("id"),
                name = if (u.isNull("name")) null else u.optString("name"),
                email = if (u.isNull("email")) null else u.optString("email"),
                image = if (u.isNull("image")) null else u.optString("image")
            )
        }
    }
}

/* ---------- katalog ---------- */

data class AnimeItem(
    val title: String,
    val animeId: String,
    val poster: String,
    val episodes: Int?,
    val score: String?,
    val status: String?
) {
    companion object {
        fun parse(o: JSONObject) = AnimeItem(
            title = o.optString("title"),
            animeId = o.optString("animeId"),
            poster = o.optString("poster"),
            episodes = if (o.isNull("episodes")) null else o.optInt("episodes"),
            score = if (o.isNull("score")) null else o.optString("score"),
            status = if (o.isNull("status")) null else o.optString("status")
        )
    }
}

data class DonghuaItem(
    val title: String,
    val slug: String,
    val poster: String,
    val status: String?,
    val currentEpisode: String?
) {
    companion object {
        fun parse(o: JSONObject) = DonghuaItem(
            title = o.optString("title"),
            slug = o.optString("slug"),
            poster = o.optString("poster"),
            status = if (o.isNull("status")) null else o.optString("status"),
            currentEpisode = if (o.isNull("currentEpisode")) null else o.optString("currentEpisode")
        )
    }
}

data class AnimeEpisodeRef(val episodeId: String, val title: String, val eps: Int?) {
    companion object {
        fun parse(o: JSONObject) = AnimeEpisodeRef(
            episodeId = o.optString("episodeId"),
            title = o.optString("title"),
            eps = if (o.isNull("eps")) null else o.optInt("eps")
        )
    }
}

data class DonghuaEpisodeRef(val slug: String, val title: String, val episodeNumber: Int?) {
    companion object {
        fun parse(o: JSONObject) = DonghuaEpisodeRef(
            slug = o.optString("slug"),
            title = o.optString("title"),
            episodeNumber = if (o.isNull("episodeNumber")) null else o.optInt("episodeNumber")
        )
    }
}

data class AnimeDetail(
    val title: String,
    val animeId: String,
    val poster: String,
    val synopsis: String?,
    val status: String?,
    val score: String?,
    val genres: List<String>,
    val episodeList: List<AnimeEpisodeRef>,
    val recommended: List<AnimeItem>
) {
    companion object {
        fun parse(o: JSONObject): AnimeDetail {
            val eps = o.optJSONArray("episodeList") ?: JSONArray()
            val rec = o.optJSONArray("recommended") ?: JSONArray()
            val genres = o.optJSONArray("genres") ?: JSONArray()
            return AnimeDetail(
                title = o.optString("title"),
                animeId = o.optString("animeId"),
                poster = o.optString("poster"),
                synopsis = if (o.isNull("synopsis")) null else o.optString("synopsis"),
                status = if (o.isNull("status")) null else o.optString("status"),
                score = if (o.isNull("score")) null else o.optString("score"),
                genres = (0 until genres.length()).map { genres.optJSONObject(it)?.optString("title") ?: "" },
                episodeList = (0 until eps.length()).mapNotNull { i ->
                    eps.optJSONObject(i)?.let(AnimeEpisodeRef::parse)
                },
                recommended = (0 until rec.length()).mapNotNull { i ->
                    rec.optJSONObject(i)?.let(AnimeItem::parse)
                }
            )
        }
    }
}

data class DonghuaDetail(
    val title: String,
    val slug: String,
    val poster: String,
    val synopsis: String?,
    val status: String?,
    val rating: String?,
    val genres: List<String>,
    val episodes: List<DonghuaEpisodeRef>
) {
    companion object {
        fun parse(o: JSONObject): DonghuaDetail {
            val eps = o.optJSONArray("episodes") ?: JSONArray()
            val genres = o.optJSONArray("genres") ?: JSONArray()
            return DonghuaDetail(
                title = o.optString("title"),
                slug = o.optString("slug"),
                poster = o.optString("poster"),
                synopsis = if (o.isNull("synopsis")) null else o.optString("synopsis"),
                status = if (o.isNull("status")) null else o.optString("status"),
                rating = if (o.isNull("rating")) null else o.optString("rating"),
                genres = (0 until genres.length()).map { genres.optJSONObject(it)?.optString("name") ?: "" },
                episodes = (0 until eps.length()).mapNotNull { i ->
                    eps.optJSONObject(i)?.let(DonghuaEpisodeRef::parse)
                }
            )
        }
    }
}

/* ---------- episode/watch ---------- */

data class AnimeServerOption(val serverId: String, val title: String) {
    companion object {
        fun parse(o: JSONObject) = AnimeServerOption(
            serverId = o.optString("serverId"),
            title = o.optString("title")
        )
    }
}

data class AnimeQualityGroup(val quality: String, val servers: List<AnimeServerOption>) {
    companion object {
        fun parse(o: JSONObject): AnimeQualityGroup {
            val arr = o.optJSONArray("servers") ?: JSONArray()
            return AnimeQualityGroup(
                quality = o.optString("quality"),
                servers = (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.let(AnimeServerOption::parse)
                }
            )
        }
    }
}

data class AnimeEpisode(
    val title: String,
    val animeId: String,
    val animeTitle: String?,
    val defaultStreamingUrl: String?,
    val prevEpisodeId: String?,
    val nextEpisodeId: String?,
    val qualities: List<AnimeQualityGroup>,
    val episodeList: List<AnimeEpisodeRef>
) {
    companion object {
        fun parse(o: JSONObject): AnimeEpisode {
            val q = o.optJSONArray("qualities") ?: JSONArray()
            val eps = o.optJSONArray("episodeList") ?: JSONArray()
            return AnimeEpisode(
                title = o.optString("title"),
                animeId = o.optString("animeId"),
                animeTitle = if (o.isNull("animeTitle")) null else o.optString("animeTitle"),
                defaultStreamingUrl = if (o.isNull("defaultStreamingUrl")) null else o.optString("defaultStreamingUrl"),
                prevEpisodeId = if (o.isNull("prevEpisodeId")) null else o.optString("prevEpisodeId"),
                nextEpisodeId = if (o.isNull("nextEpisodeId")) null else o.optString("nextEpisodeId"),
                qualities = (0 until q.length()).mapNotNull { i ->
                    q.optJSONObject(i)?.let(AnimeQualityGroup::parse)
                },
                episodeList = (0 until eps.length()).mapNotNull { i ->
                    eps.optJSONObject(i)?.let(AnimeEpisodeRef::parse)
                }
            )
        }
    }
}

data class DonghuaStreamServer(val name: String, val url: String) {
    companion object {
        fun parse(o: JSONObject) = DonghuaStreamServer(
            name = o.optString("name"),
            url = o.optString("url")
        )
    }
}

data class DonghuaEpisode(
    val title: String,
    val donghuaTitle: String?,
    val donghuaSlug: String?,
    val poster: String?,
    val servers: List<DonghuaStreamServer>,
    val prevEpisodeSlug: String?,
    val nextEpisodeSlug: String?
) {
    companion object {
        fun parse(o: JSONObject): DonghuaEpisode {
            val arr = o.optJSONArray("servers") ?: JSONArray()
            return DonghuaEpisode(
                title = o.optString("title"),
                donghuaTitle = if (o.isNull("donghuaTitle")) null else o.optString("donghuaTitle"),
                donghuaSlug = if (o.isNull("donghuaSlug")) null else o.optString("donghuaSlug"),
                poster = if (o.isNull("poster")) null else o.optString("poster"),
                servers = (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.let(DonghuaStreamServer::parse)
                },
                prevEpisodeSlug = if (o.isNull("prevEpisodeSlug")) null else o.optString("prevEpisodeSlug"),
                nextEpisodeSlug = if (o.isNull("nextEpisodeSlug")) null else o.optString("nextEpisodeSlug")
            )
        }
    }
}

/* ---------- aktivitas user ---------- */

data class WatchProgress(
    val contentId: String,
    val type: String,
    val episodeId: String,
    val episode: Int?,
    val title: String,
    val poster: String
) {
    companion object {
        fun parse(o: JSONObject) = WatchProgress(
            contentId = o.optString("contentId"),
            type = o.optString("type"),
            episodeId = o.optString("episodeId"),
            episode = if (o.isNull("episode")) null else o.optInt("episode"),
            title = o.optString("title"),
            poster = o.optString("poster")
        )
    }
}

data class HistoryEntry(
    val contentId: String,
    val type: String,
    val title: String,
    val poster: String,
    val episode: Int?,
    val watchedAt: Long
) {
    companion object {
        fun parse(o: JSONObject) = HistoryEntry(
            contentId = o.optString("contentId"),
            type = o.optString("type"),
            title = o.optString("title"),
            poster = o.optString("poster"),
            episode = if (o.isNull("episode")) null else o.optInt("episode"),
            watchedAt = o.optLong("watchedAt", 0L)
        )
    }
}

data class Favorite(
    val contentId: String,
    val title: String,
    val poster: String
) {
    companion object {
        fun parse(o: JSONObject) = Favorite(
            contentId = o.optString("contentId"),
            title = o.optString("title"),
            poster = o.optString("poster")
        )
    }
}

data class SearchResults(
    val anime: List<Pair<String, String>>, // title, animeId
    val donghua: List<Pair<String, String>> // title, slug
)

data class NotifyPrefs(
    val newEpisode: Boolean,
    val favorite: Boolean,
    val announcement: Boolean,
    val maintenance: Boolean,
    val appUpdate: Boolean
) {
    companion object {
        val DEFAULT = NotifyPrefs(true, true, true, true, true)

        fun parse(o: JSONObject) = NotifyPrefs(
            newEpisode = o.optBoolean("newEpisode", true),
            favorite = o.optBoolean("favorite", true),
            announcement = o.optBoolean("announcement", true),
            maintenance = o.optBoolean("maintenance", true),
            appUpdate = o.optBoolean("appUpdate", true)
        )
    }
}
