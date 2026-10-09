package id.my.id.cyronime.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Ekstraksi direct file (HLS/MP4) dari halaman embed, DIJALANKAN DI HP.
 *
 * Kenapa di HP, bukan di backend: URL m3u8 vidhide membawa token yang terikat
 * ASN/IP peminta (param `asn=`). Kalau backend (Vercel/AWS) yang mengekstrak,
 * HP (ISP Indonesia) ditolak CDN dengan 403 sehingga ExoPlayer gagal. Dengan
 * mengambil halaman embed langsung dari HP, token cocok dengan jaringan HP.
 *
 * Port dari kamael/src/lib/api/embed-extract.ts. Packer Dean Edwards di-unpack
 * murni (tanpa eval / tanpa menjalankan JS hasil scrape).
 */
object EmbedExtractor {
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    private const val MAX_HTML = 2_000_000

    private val http = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class Result(val url: String, val type: String, val host: String, val referer: String)

    /** Host yang tidak bisa diekstrak (terenkripsi/diproteksi) -> langsung WebView. */
    fun isExtractable(embedUrl: String): Boolean {
        val h = try { URI(embedUrl).host?.lowercase() ?: return false } catch (_: Exception) { return false }
        return !(h.endsWith("mega.nz") || h.endsWith("mega.io"))
    }

    fun extract(embedUrl: String): Result? {
        if (!embedUrl.startsWith("http")) return null
        if (!isExtractable(embedUrl)) return null
        val (html, finalUrl) = fetch(embedUrl) ?: return null
        val origin = try {
            val u = URI(finalUrl); "${u.scheme}://${u.host}" + (if (u.port > 0) ":${u.port}" else "")
        } catch (_: Exception) { return null }
        val host = try { URI(finalUrl).host ?: "" } catch (_: Exception) { "" }

        val haystacks = listOf(unpack(html) ?: "", html)
        var best: Cand? = null
        for (js in haystacks) {
            if (js.isEmpty()) continue
            val top = collect(js, finalUrl).maxByOrNull { it.score } ?: continue
            if (best == null || top.score > best.score) best = top
            if (best.score >= 4) break
        }
        val b = best ?: return null
        if (b.score < 1) return null
        return Result(b.url, b.type, host, "$origin/")
    }

    private fun fetch(url: String): Pair<String, String>? = try {
        val req = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("Accept", "text/html,*/*")
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val body = r.body ?: return null
            val bytes = body.bytes()
            if (bytes.size > MAX_HTML) return null
            String(bytes, Charsets.UTF_8) to r.request.url.toString()
        }
    } catch (_: Exception) { null }

    /* ---------------- unpack Dean Edwards packer ---------------- */

    private fun unpack(html: String): String? {
        val start = html.indexOf("eval(function(p,a,c,k,e,d)")
        if (start < 0) return null
        // cari awal argumen: }('PAYLOAD',a,c,'k1|k2'.split('|'),...
        val argStart = html.indexOf("}('", start)
        if (argStart < 0) return null
        var i = argStart + 3
        val sb = StringBuilder()
        while (i < html.length && html[i] != '\'') {
            if (html[i] == '\\' && i + 1 < html.length) {
                when (val n = html[i + 1]) {
                    '\'' -> sb.append('\'')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '/' -> sb.append('/')
                    else -> { sb.append('\\'); sb.append(n) }
                }
                i += 2
            } else { sb.append(html[i]); i++ }
        }
        var p = sb.toString()
        i++ // tutup quote payload
        val m = Regex("""^,\s*(\d+)\s*,\s*(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'\s*\.split\('\|'\)""")
            .find(html.substring(i, minOf(html.length, i + 200_000))) ?: return null
        val a = m.groupValues[1].toInt()
        val c = m.groupValues[2].toInt()
        if (a < 2 || a > 36 || c < 1 || c > 5000) return null
        val k = m.groupValues[3].split("|")
        for (idx in c - 1 downTo 0) {
            val word = k.getOrNull(idx)
            if (word.isNullOrEmpty()) continue
            val token = idx.toString(a)
            p = Regex("\\b" + Regex.escape(token) + "\\b").replace(p) { word }
        }
        return p
    }

    /* ---------------- kandidat media ---------------- */

    private data class Cand(val url: String, val type: String, val score: Int)

    private val absRe = Regex("""["'`](https?://[^"'`\\\s]*(?:\.m3u8|\.mp4|\.m4v|\.webm)[^"'`\\\s]*)["'`]""", RegexOption.IGNORE_CASE)
    private val relRe = Regex("""["'`](/[^"'`\\\s]*(?:\.m3u8|\.mp4|\.m4v|\.webm)[^"'`\\\s]*)["'`]""", RegexOption.IGNORE_CASE)

    private fun score(type: String, path: String): Int {
        if (type == "mp4") return 0
        if (Regex("master\\.m3u8", RegexOption.IGNORE_CASE).containsMatchIn(path)) return 4
        if (Regex("index\\.m3u8", RegexOption.IGNORE_CASE).containsMatchIn(path)) return 3
        if (Regex("iframes?[-.]", RegexOption.IGNORE_CASE).containsMatchIn(path)) return 1
        return 2
    }

    private fun collect(js: String, base: String): List<Cand> {
        val out = ArrayList<Cand>()
        fun push(raw: String) {
            val l = raw.lowercase()
            val hls = l.contains(".m3u8")
            val mp4 = l.contains(".mp4") || l.contains(".m4v") || l.contains(".webm")
            if (!hls && !mp4) return
            val abs = try { URI(base).resolve(raw) } catch (_: Exception) { return }
            if (abs.scheme != "http" && abs.scheme != "https") return
            val type = if (hls) "hls" else "mp4"
            out.add(Cand(abs.toString(), type, score(type, abs.path ?: "")))
        }
        absRe.findAll(js).forEach { push(it.groupValues[1]) }
        relRe.findAll(js).forEach { push(it.groupValues[1]) }
        return out
    }
}
