package id.my.id.cyronime.app.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import id.my.id.cyronime.app.data.AnimeEpisode
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.DonghuaEpisode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Watch: sumber HLS/MP4 diputar ExoPlayer; sumber embed (vidhide dsb.)
 * dirender WebView — arsitektur yang sama dengan Web (embed iframe).
 * Progress tercatat ke backend (Continue Watching + History) dengan
 * akun yang sama seperti Web.
 */
@Composable
fun WatchScreen(nav: NavController, type: String, id: String) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var anime by remember { mutableStateOf<AnimeEpisode?>(null) }
    var donghua by remember { mutableStateOf<DonghuaEpisode?>(null) }

    // Server aktif: anime = "quality|serverId", donghua = nama server.
    var activeServerKey by remember { mutableStateOf<String?>(null) }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var streamIsEmbed by remember { mutableStateOf(true) }

    fun resolveAnimeServer(serverId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = Api.resolveServer(serverId)
                if (url.isNotBlank()) {
                    streamUrl = url
                    streamIsEmbed = !isDirectVideo(url)
                } else {
                    error = "Server streaming tidak tersedia."
                }
            } catch (err: Exception) {
                error = errorMessage(err)
            }
        }
    }

    fun load() {
        loading = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (type == "anime") {
                    val ep = Api.animeEpisode(id)
                    anime = ep
                    // Default: server pertama dari kualitas pertama.
                    val server = ep.qualities.firstOrNull()?.servers?.firstOrNull()
                    if (server != null) {
                        activeServerKey = "${ep.qualities.first().quality}|${server.serverId}|${server.title}"
                        resolveAnimeServer(server.serverId)
                    } else if (!ep.defaultStreamingUrl.isNullOrBlank()) {
                        streamUrl = ep.defaultStreamingUrl
                        streamIsEmbed = !isDirectVideo(ep.defaultStreamingUrl!!)
                    } else {
                        error = "Tidak ada server streaming untuk episode ini."
                    }
                    // Catat progress (episode terakhir, bukan detik — sama seperti Web).
                    Api.postProgress(
                        contentId = ep.animeId,
                        type = "anime",
                        episodeId = id,
                        episode = ep.epsOf(id),
                        title = ep.animeTitle ?: ep.title,
                        poster = ""
                    )
                } else {
                    val ep = Api.donghuaEpisode(id)
                    donghua = ep
                    val first = ep.servers.firstOrNull()
                    if (first != null) {
                        activeServerKey = first.name
                        streamUrl = first.url
                        streamIsEmbed = !isDirectVideo(first.url)
                    } else {
                        error = "Tidak ada server streaming untuk episode ini."
                    }
                    Api.postProgress(
                        contentId = ep.donghuaSlug ?: id,
                        type = "donghua",
                        episodeId = id,
                        episode = null,
                        title = ep.donghuaTitle ?: ep.title,
                        poster = ep.poster ?: ""
                    )
                }
            } catch (err: Exception) {
                error = errorMessage(err)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        if (loading) {
            LoadingScreen("Menyiapkan player…")
            return@Column
        }
        error?.let { message ->
            ErrorScreen(message, retry = { load() })
            return@Column
        }

        val url = streamUrl
        if (url != null) {
            if (streamIsEmbed) EmbedPlayer(url) else NativePlayer(url)
        } else {
            Text(
                "Server belum dipilih.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        val ep = anime
        val dep = donghua
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(ep?.title ?: dep?.title ?: "", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Pemilih server
            val chips = remember(ep, dep) {
                if (ep != null) {
                    ep.qualities.flatMap { q ->
                        q.servers.map { s -> Triple("${q.quality} • ${s.title}", "${q.quality}|${s.serverId}|${s.title}", s.serverId) }
                    }
                } else if (dep != null) {
                    dep.servers.map { Triple(it.name, it.name, it.url) }
                } else emptyList()
            }
            if (chips.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chips) { (label, key, value) ->
                        FilterChip(
                            selected = activeServerKey == key,
                            onClick = {
                                activeServerKey = key
                                if (ep != null) resolveAnimeServer(value)
                                else {
                                    streamUrl = value
                                    streamIsEmbed = !isDirectVideo(value)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Navigasi prev/next
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val prev = ep?.prevEpisodeId ?: dep?.prevEpisodeSlug
                val next = ep?.nextEpisodeId ?: dep?.nextEpisodeSlug
                if (prev != null) {
                    OutlinedButton(onClick = { nav.navigate("watch/$type/$prev") { popUpTo("watch/$type/$id") { inclusive = true } } }) {
                        Text("◀ Episode sebelumnya")
                    }
                }
                if (next != null) {
                    OutlinedButton(onClick = { nav.navigate("watch/$type/$next") { popUpTo("watch/$type/$id") { inclusive = true } } }) {
                        Text("Episode berikutnya ▶")
                    }
                }
            }

            // Daftar episode (lompat cepat)
            if (ep != null && ep.episodeList.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Semua episode", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ep.episodeList.reversed()) { e ->
                        FilterChip(
                            selected = e.episodeId == id,
                            onClick = { nav.navigate("watch/anime/${e.episodeId}") },
                            label = { Text(e.eps?.let { "Ep $it" } ?: e.title.take(14)) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun AnimeEpisode.epsOf(currentId: String): Int? =
    episodeList.firstOrNull { it.episodeId == currentId }?.eps

/** URL langsung (.m3u8 / .mp4 / .webm) diputar ExoPlayer; selain itu embed. */
private fun isDirectVideo(url: String): Boolean {
    val clean = url.split("?").firstOrNull()?.lowercase() ?: return false
    return clean.contains(".m3u8") || clean.endsWith(".mp4") || clean.endsWith(".m4v") || clean.endsWith(".webm")
}

@Composable
private fun NativePlayer(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    androidx.compose.runtime.DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
    )
}

@Composable
private fun EmbedPlayer(url: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
    )
}
