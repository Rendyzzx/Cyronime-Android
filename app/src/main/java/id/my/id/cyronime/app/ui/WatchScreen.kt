package id.my.id.cyronime.app.ui

import id.my.id.cyronime.app.R

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import id.my.id.cyronime.app.data.AnimeEpisode
import id.my.id.cyronime.app.data.AnimeServerOption
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.DonghuaEpisode
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Watch ala /anime/watch/[episode] web: bar atas -> player 16:9 full-bleed
 * (ExoPlayer native utk HLS/MP4 dengan kontrol meniru components/player,
 * WebView utk embed pihak ketiga) -> info episode -> aksi -> strip episode.
 * Resume & progres pakai endpoint /api/watch/progress yang sama dengan web.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(nav: NavController, type: String, id: String) {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var anime by remember { mutableStateOf<AnimeEpisode?>(null) }
    var donghua by remember { mutableStateOf<DonghuaEpisode?>(null) }
    var favorite by remember { mutableStateOf(false) }

    // Sumber aktif
    var activeQuality by remember { mutableStateOf<String?>(null) }
    var activeServerKey by remember { mutableStateOf<String?>(null) }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var streamIsEmbed by remember { mutableStateOf(true) }
    var streamReferer by remember { mutableStateOf("") }
    var resolving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var posterForProgress by remember { mutableStateOf("") }
    var allFailed by remember { mutableStateOf(false) }
    val failedServers = remember { mutableStateOf(setOf<String>()) }
    var sheetOpen by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }

    fun qualityLabel(q: String?): String = when {
        q == null -> "Auto"
        q.contains("1080") -> "1080p"
        q.contains("720") -> "720p"
        q.contains("480") -> "480p"
        q.contains("360") -> "360p"
        else -> q
    }

    /** Mega terenkripsi (WebView hitam) -> selalu dicoba paling akhir. */
    fun isMega(title: String) = title.contains("mega", ignoreCase = true)

    /**
     * Peringkat server (kecil = dicoba duluan): vidhide bisa diekstrak -> diputar
     * ExoPlayer native (0); server embed lain (desustream dll) hanya lewat
     * WebView yang sering layar hitam (1); Mega terenkripsi, hitam (2).
     */
    fun rank(title: String): Int = when {
        title.contains("vidhide", ignoreCase = true) -> 0
        isMega(title) -> 2
        else -> 1
    }

    /** Pilih server terbaik: kualitas yang diminta dulu (bila ada), lalu semua kualitas. */
    fun pickBest(
        pool: List<Pair<String, AnimeServerOption>>, preferQuality: String?
    ): Pair<String, AnimeServerOption>? {
        val q = preferQuality?.let { pq -> pool.filter { it.first == pq } }.orEmpty()
        val bestInQ = q.minByOrNull { rank(it.second.title) }
        val bestAll = pool.minByOrNull { rank(it.second.title) }
        return bestInQ ?: bestAll
    }

    fun epsOf(): Int? = anime?.episodeList?.firstOrNull { it.episodeId == id }?.eps

    /** Coba satu server anime; gagal -> fallback ke server lain (kualitas sama dulu). */
    val io = rememberIoScope()

    fun tryAnimeServer(quality: String, serverId: String, serverTitle: String) {
        activeQuality = quality
        activeServerKey = serverId
        statusMessage = "Memuat $serverTitle (${qualityLabel(quality)})…"
        resolving = true
        io.launch {
            try {
                val url = Api.resolveServer(serverId)
                if (url.isNotBlank()) {
                    // URL embed (vidhide/desustream/mega) -> minta backend
                    // ekstrak direct file (.m3u8/.mp4) supaya diputar
                    // ExoPlayer native (WebView embed sering layar hitam).
                    // Gagal ekstrak -> WebView embed seperti sebelumnya.
                    var target = url
                    var embed = !isDirectVideo(url)
                    if (embed) {
                        val direct = Api.extractStream(url, serverId)
                        if (direct != null && isDirectVideo(direct.url)) {
                            target = direct.url
                            embed = false
                            streamReferer = direct.referer
                        }
                    }
                    streamUrl = target
                    streamIsEmbed = embed
                    allFailed = false
                } else throw IllegalStateException("empty")
            } catch (_: Exception) {
                failedServers.value = failedServers.value + serverId
                val groups = anime?.qualities ?: emptyList()
                val pool = groups.flatMap { g -> g.servers.map { g.quality to it } }
                    .filter { it.second.serverId !in failedServers.value }
                // Fallback: server terbaik di kualitas sama; kalau habis, kualitas lain.
                val best = pickBest(pool, quality)
                if (best != null) {
                    tryAnimeServer(best.first, best.second.serverId, best.second.title)
                    return@launch
                }
                streamUrl = null
                allFailed = true
                statusMessage = null
            } finally {
                resolving = false
            }
        }
    }

    fun useDonghuaServer(name: String, url: String) {
        activeQuality = null
        activeServerKey = name
        streamUrl = url
        streamIsEmbed = !isDirectVideo(url)
    }

    /** Dipanggil NativePlayer saat frame pertama siap: hapus pil "Mencoba…". */
    fun onPlaybackReady() { statusMessage = null; allFailed = false }

    /** ExoPlayer gagal memutar URL ini -> perlakukan server aktif sebagai gagal & coba berikutnya. */
    fun onPlaybackFatal() {
        val key = activeServerKey ?: return
        if (type == "anime") {
            val groups = anime?.qualities ?: emptyList()
            failedServers.value = failedServers.value + key
            val remaining = groups.flatMap { g -> g.servers.map { g.quality to it } }
                .filter { (_, sv) -> sv.serverId !in failedServers.value }
            val next = pickBest(remaining, activeQuality)
            if (next != null) {
                streamUrl = null
                tryAnimeServer(next.first, next.second.serverId, next.second.title)
            } else {
                statusMessage = null
                allFailed = true
            }
        } else {
            val servers = donghua?.servers ?: emptyList()
            failedServers.value = failedServers.value + key
            val next = servers.firstOrNull { it.name !in failedServers.value }
            if (next != null) useDonghuaServer(next.name, next.url) else allFailed = true
        }
    }

    fun load() {
        loading = true
        error = null
        failedServers.value = emptySet()
        allFailed = false
        io.launch {
            try {
                if (type == "anime") {
                    val ep = Api.animeEpisode(id)
                    anime = ep
                    val ordered = ep.qualities.flatMap { g -> g.servers.map { g.quality to it } }
                    // Awal: server terbaik di SEMUA kualitas (vidhide dulu), lalu
                    // user bisa ganti kualitas lewat menu.
                    val first = pickBest(ordered, null)
                    when {
                        first != null -> tryAnimeServer(
                            first.first, first.second.serverId, first.second.title
                        )
                        !ep.defaultStreamingUrl.isNullOrBlank() -> {
                            streamUrl = ep.defaultStreamingUrl
                            streamIsEmbed = !isDirectVideo(ep.defaultStreamingUrl!!)
                        }
                        else -> error = "Tidak ada server streaming untuk episode ini."
                    }
                    // Poster untuk kartu "Lanjut nonton": ambil dari detail (cache 2 menit).
                    val posterUrl = try { Api.animeDetail(ep.animeId).poster } catch (_: Exception) { "" }
                    posterForProgress = posterUrl
                    Api.postProgress(
                        contentId = ep.animeId, type = "anime", episodeId = id,
                        episode = ep.epsOf(id), title = ep.animeTitle ?: ep.title, poster = posterUrl
                    )
                    favorite = Api.favorites("anime").any { it.contentId == ep.animeId }
                } else {
                    val ep = Api.donghuaEpisode(id)
                    donghua = ep
                    val first = ep.servers.firstOrNull()
                    if (first != null) {
                        useDonghuaServer(first.name, first.url)
                    } else {
                        error = "Tidak ada server streaming untuk episode ini."
                    }
                    Api.postProgress(
                        contentId = ep.donghuaSlug ?: id, type = "donghua", episodeId = id,
                        episode = null, title = ep.donghuaTitle ?: ep.title, poster = ep.poster ?: ""
                    )
                    favorite = Api.favorites("donghua").any { it.contentId == (ep.donghuaSlug ?: id) }
                }
            } catch (err: Exception) {
                error = errorMessage(err)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    fun toggleFavorite() {
        val ep = anime
        val dep = donghua
        val contentId = ep?.animeId ?: dep?.donghuaSlug ?: return
        val title = ep?.animeTitle ?: ep?.title ?: dep?.donghuaTitle ?: dep?.title ?: ""
        val poster = dep?.poster ?: ""
        val next = !favorite
        io.launch {
            try {
                if (next) Api.addFavorite(type, contentId, title, poster)
                else Api.removeFavorite(type, contentId)
                favorite = next
            } catch (_: Exception) {
            }
        }
    }

    val ep = anime
    val dep = donghua
    val seriesTitle = ep?.animeTitle ?: ep?.title?.substringBefore(" Episode")
        ?: dep?.donghuaTitle ?: dep?.title ?: ""
    val epNumber = epsOf()
    val shortLabel = if (epNumber != null) "Episode $epNumber" else (ep?.title ?: dep?.title ?: "")
    val prevId = ep?.prevEpisodeId ?: dep?.prevEpisodeSlug
    val nextId = ep?.nextEpisodeId ?: dep?.nextEpisodeSlug
    val contentId = ep?.animeId ?: dep?.donghuaSlug ?: ""

    // Orientasi layar penuh (ala useFullscreenLock web) + mode immersive:
    // sembunyikan status bar (jam/wifi) & navigation bar saat fullscreen.
    DisposableEffect(fullscreen) {
        val activity = ctx as? Activity
        activity?.requestedOrientation = if (fullscreen)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        val window = activity?.window
        if (window != null) {
            val ctrl = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            ctrl.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (fullscreen) ctrl.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            else ctrl.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            val w = (ctx as? Activity)?.window
            w?.let {
                androidx.core.view.WindowCompat.getInsetsController(it, it.decorView)
                    .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            (ctx as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    /* ---------- Player box (dipakai mode biasa & fullscreen) ---------- */
    @Composable
    fun PlayerBox(modifier: Modifier) {
        Box(modifier.background(Color.Black)) {
            val url = streamUrl
            if (url != null) {
                if (streamIsEmbed) {
                    EmbedPlayer(
                        url,
                        onLoaded = { onPlaybackReady() },
                        onFatal = { onPlaybackFatal() }
                    )
                    // WebView embed tak punya kontrol sendiri (mis. Mega) ->
                    // tombol ganti server (kiri) + layar penuh (kanan),
                    // aktif juga di mode fullscreen/landscape.
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x99_000000))
                            .clickable { sheetOpen = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Server", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x99_000000))
                            .clickable { fullscreen = !fullscreen },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "Layar penuh / putar",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // key(url): ExoPlayer hanya dibuat ulang bila URL berganti,
                    // bukan setiap state induk (pil status dll) berubah.
                    androidx.compose.runtime.key(url) {
                    NativePlayer(
                        url = url,
                        referer = streamReferer,
                        contentId = contentId,
                        type = type,
                        id = id,
                        episode = epNumber,
                        seriesTitle = seriesTitle,
                        prevId = prevId,
                        nextId = nextId,
                        nav = nav,
                        qualityLabel = qualityLabel(activeQuality),
                        poster = posterForProgress,
                        onOpenSettings = { sheetOpen = true },
                        onFullscreen = { fullscreen = !fullscreen },
                        onReady = { onPlaybackReady() },
                        onFatalError = { onPlaybackFatal() }
                    )
                    }
                }
            }

            // Pil status (spinner + pesan) ala PlayerChrome.StatusPill
            statusMessage?.let { msg ->
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Cy.Overlay)
                        .border(1.dp, Cy.Surface2, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = Cy.Text2, strokeWidth = 2.dp, modifier = Modifier.size(14.dp)
                    )
                    Text(msg, color = Cy.Text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (resolving && streamUrl == null && statusMessage == null) {
                CircularProgressIndicator(
                    color = Cy.Text2, strokeWidth = 3.dp,
                    modifier = Modifier.align(Alignment.Center).size(36.dp)
                )
            }

            // Overlay error ala PlayerChrome.ErrorOverlay
            if (allFailed) {
                Box(
                    Modifier.fillMaxSize().background(Cy.Overlay),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Semua server ${qualityLabel(activeQuality)} gagal dimuat",
                            color = Cy.Text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Coba lagi, atau pakai kualitas yang masih tersedia.",
                            color = Cy.Text2, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Cy.Text)
                                    .clickable { load() }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Coba lagi", color = Cy.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            val groups = ep?.qualities ?: emptyList()
                            val other = groups.firstOrNull { g -> g.servers.any { it.serverId !in failedServers.value } }
                            if (other != null) {
                                Box(
                                    Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(50))
                                        .border(1.dp, Cy.Surface2, RoundedCornerShape(50))
                                        .clickable {
                                            allFailed = false
                                            val s = other.servers.first { it.serverId !in failedServers.value }
                                            tryAnimeServer(other.quality, s.serverId, s.title)
                                        }
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Pakai ${qualityLabel(other.quality)}",
                                        color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // movableContent: PlayerBox boleh pindah posisi layout (portrait ->
    // fullscreen) TANPA dibuat ulang — tanpa ini ExoPlayer/WebView di-dispose
    // tiap toggle fullscreen dan video mundur ke detik awal.
    val playerBox = remember {
        androidx.compose.runtime.movableContentOf<Modifier> { mod -> PlayerBox(mod) }
    }

    when {
        loading -> LoadingScreen("Menyiapkan player…")
        error != null -> ErrorScreen(error!!, retry = { load() })
        fullscreen -> Box(Modifier.fillMaxSize()) { playerBox(Modifier.fillMaxSize()) }
        else -> Column(Modifier.fillMaxSize()) {
            /* ---------- Bar atas: back + Portal ---------- */
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Cy.Navy)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { nav.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Kembali",
                        tint = Cy.Text, modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    "Portal ${if (type == "anime") "Anime" else "Donghua"}",
                    color = Cy.Text2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { nav.navigate("portal/$type") }
                )
            }

            /* ---------- Player 16:9 ---------- */
            playerBox(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            /* ---------- Konten bawah player ---------- */
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(18.dp))
                // Info episode
                Text(
                    seriesTitle,
                    color = Cy.Text2, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = contentId.isNotBlank()) {
                        nav.navigate("detail/$type/$contentId")
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    shortLabel,
                    color = Cy.Text, fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp
                )
                if (!ep?.title.isNullOrBlank() && ep?.title != shortLabel) {
                    Spacer(Modifier.height(6.dp))
                    Text(ep?.title ?: "", color = Cy.Text2, fontSize = 16.sp)
                }

                // Aksi: simpan favorit + pilih server
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(Cy.RadiusChip))
                            .background(Cy.Surface)
                            .clickable { toggleFavorite() }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Simpan", tint = if (favorite) Cy.Peach else Cy.Text2,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan", color = Cy.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(Cy.RadiusChip))
                            .background(Cy.Surface)
                            .clickable { sheetOpen = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, "Server", tint = Cy.Text2, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            activeServerKey?.let { "Server: $it" } ?: "Pilih server",
                            color = Cy.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                /* ---------- Strip episode horizontal ala EpisodeStrip ---------- */
                val eps = ep?.episodeList ?: emptyList()
                if (eps.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    val stripState = rememberLazyListState()
                    LaunchedEffect(id) {
                        val idx = eps.indexOfFirst { it.episodeId == id }
                        if (idx > 0) stripState.scrollToItem(idx)
                    }
                    LazyRow(
                        state = stripState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        itemsIndexed(eps, key = { _, e -> e.episodeId }) { _, e ->
                            val active = e.episodeId == id
                            val watched = !active && e.eps != null && epNumber != null && e.eps < epNumber
                            Box(
                                Modifier
                                    .size(width = 72.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(Cy.RadiusPlayer))
                                    .background(Cy.Surface)
                                    .border(
                                        width = if (active) 2.dp else 0.dp,
                                        color = if (active) Cy.Text2 else Color.Transparent,
                                        shape = RoundedCornerShape(Cy.RadiusPlayer)
                                    )
                                    .clickable { nav.navigate("watch/$type/${e.episodeId}") }
                            ) {
                                if (watched) Box(Modifier.fillMaxSize().background(Color(0x73_000000)))
                                Text(
                                    e.eps?.toString() ?: "•",
                                    color = Cy.Text,
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Sheet kualitas / server (ala SettingsSheet web). Di LUAR cabang when
    // supaya bisa dibuka juga saat fullscreen/landscape (fix: dulu sheet
    // hanya dirender di cabang portrait -> tidak bisa ganti resolusi).
    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            containerColor = Cy.Surface,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                val groups = ep?.qualities ?: emptyList()
                if (groups.isNotEmpty()) {
                    Text("Kualitas", color = Cy.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    groups.forEach { g ->
                        val isActive = activeQuality == g.quality &&
                            g.servers.any { it.serverId == activeServerKey }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Cy.RadiusChip))
                                .background(if (isActive) Cy.Accent else Cy.Surface2)
                                .clickable {
                                    sheetOpen = false
                                    // Dalam kualitas pilihan: vidhide > embed lain > Mega,
                                    // server yang sudah gagal dilewati.
                                    val s = g.servers.filter { it.serverId !in failedServers.value }
                                        .minByOrNull { rank(it.title) }
                                        ?: g.servers.minByOrNull { rank(it.title) } ?: return@clickable
                                    tryAnimeServer(g.quality, s.serverId, s.title)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                qualityLabel(g.quality), color = Cy.Text,
                                fontSize = 14.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (dep != null && dep.servers.isNotEmpty()) {
                    Text("Server", color = Cy.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    dep.servers.forEach { s ->
                        val isActive = activeServerKey == s.name
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Cy.RadiusChip))
                                .background(if (isActive) Cy.Accent else Cy.Surface2)
                                .clickable {
                                    sheetOpen = false
                                    useDonghuaServer(s.name, s.url)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                s.name, color = Cy.Text, fontSize = 14.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
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

/* ============================================================
 * NativePlayer — kontrol meniru components/player/NativePlayer.tsx:
 * baris tengah Prev/-10/Play/+10/Next, baris sekunder (pill kualitas,
 * pill kecepatan, layar penuh), waktu + seekbar tipis glacier,
 * auto-hide 3 dtk, resume dari posisi terakhir, simpan progres 5 dtk.
 * ============================================================ */

@Composable
private fun NativePlayer(
    url: String,
    referer: String = "",
    contentId: String,
    type: String,
    id: String,
    episode: Int?,
    seriesTitle: String,
    prevId: String?,
    nextId: String?,
    nav: NavController,
    qualityLabel: String,
    poster: String = "",
    onOpenSettings: () -> Unit,
    onFullscreen: () -> Unit,
    onReady: () -> Unit,
    onFatalError: () -> Unit
) {
    val context = LocalContext.current
    var speed by remember { mutableStateOf(1.0f) }
    var playing by remember { mutableStateOf(false) }
    var current by remember { mutableDoubleStateOf(0.0) }
    var duration by remember { mutableDoubleStateOf(0.0) }
    var buffered by remember { mutableDoubleStateOf(0.0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var spin by remember { mutableStateOf(true) }
    var skipFlash by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var controlsShownAt by remember { mutableStateOf(0L) }
    var resumeDone by remember { mutableStateOf(false) }
    val io = rememberIoScope()
    var playerError by remember { mutableStateOf<String?>(null) }

    val player = remember(url) {
        // Data source OkHttp: UA browser, TANPA Referer (sama seperti <video>
        // di web), redirect http<->https diikuti. Tanpa ini sebagian host
        // video menolak request default ExoPlayer -> layar kosong.
        val http = okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val dataSource = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(http)
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            )
            .setDefaultRequestProperties(
                if (referer.isNotBlank())
                    mapOf("Referer" to referer, "Origin" to referer.trimEnd('/'))
                else emptyMap()
            )
        val load = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* min */ 15_000, /* max */ 50_000,
                /* bufferForPlayback */ 500, /* afterRebuffer */ 1_500
            )
            .build()
        val bandwidth = androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(1_500_000L)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(load)
            .setBandwidthMeter(bandwidth)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSource)
            )
            .build().apply {
            // Audio focus: otomatis pause/duck saat panggilan masuk dll.
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    // Kecepatan playback
    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }

    // Listener state player
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (isPlaying) spin = false
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) { spin = false; onReady() }
                if (state == Player.STATE_BUFFERING && playing) spin = true
            }
            override fun onPlayerError(err: PlaybackException) {
                spin = false
                playerError = "Video gagal dimuat (${err.errorCodeName}). " +
                    "Mencoba server lain…"
                // Minta layar induk pindah ke server berikutnya (fallback otomatis).
                onFatalError()
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }

    // Putar LANGSUNG (prepare sudah jalan); resume diambil paralel dengan
    // timeout 2 dtk lalu seek bila ada, supaya loading tidak menunggu backend.
    LaunchedEffect(url) {
        if (resumeDone) return@LaunchedEffect
        resumeDone = true
        player.play()
        if (contentId.isNotBlank()) {
            val resume = kotlinx.coroutines.withTimeoutOrNull(2000) {
                WatchProgressSync.fetchResume(contentId)
            }
            // position/duration dari backend dalam DETIK
            if (resume != null && resume.episodeId == id && resume.position > 10 &&
                resume.position < resume.duration - 30
            ) {
                player.seekTo((resume.position * 1000).toLong())
            }
        }
    }

    // Poll posisi + auto-hide kontrol 3 dtk + simpan progres tiap 5 dtk
    LaunchedEffect(player) {
        var lastSave = 0L
        while (isActive) {
            delay(500)
            // Internal UI & backend pakai DETIK; ExoPlayer pakai milidetik.
            current = (player.currentPosition / 1000.0).coerceAtLeast(0.0)
            val d = player.duration
            duration = if (d > 0) d / 1000.0 else 0.0
            buffered = (player.bufferedPosition / 1000.0).coerceAtLeast(0.0)
            val now = System.currentTimeMillis()
            if (playing && controlsVisible && controlsShownAt > 0 && now - controlsShownAt > 3000) {
                controlsVisible = false
            }
            if (playing && duration > 0 && contentId.isNotBlank() && now - lastSave >= 5000) {
                lastSave = now
                val pos = player.currentPosition / 1000
                val dur = player.duration / 1000
                if (dur > 0) {
                    io.launch {
                        WatchProgressSync.save(
                            contentId = contentId, type = type, episodeId = id,
                            episode = episode, title = seriesTitle, poster = poster,
                            position = pos, duration = dur
                        )
                    }
                }
            }
        }
    }

    fun skip(seconds: Int) {
        val target = (player.currentPosition + seconds * 1000).coerceIn(0, player.duration)
        player.seekTo(target)
        skipFlash = if (seconds < 0) "-10 dtk" else "+10 dtk"
        controlsShownAt = System.currentTimeMillis()
    }

    /** Coba lagi setelah error: bersihkan state lalu prepare ulang. */
    fun retryPlayback() {
        playerError = null
        spin = true
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
        controlsVisible = true
        controlsShownAt = System.currentTimeMillis()
    }

    fun toggleControls() {
        controlsVisible = !controlsVisible
        controlsShownAt = System.currentTimeMillis()
    }

    skipFlash?.let { flash ->
        LaunchedEffect(flash) {
            delay(700)
            skipFlash = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                // TextureView (bukan SurfaceView) + FIT: SurfaceView di dalam
                // Compose sering ter-layout gepeng/0 tinggi. Ukuran dipaksa MATCH_PARENT.
                (android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.player_texture_view, null) as PlayerView).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay tap (di atas video): toggle kontrol ala web
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { toggleControls() },
                        onDoubleTap = { offset ->
                            // Sisi kiri mundur 10 dtk, sisi kanan maju 10 dtk (ala web)
                            if (offset.x < size.width / 2) skip(-10) else skip(10)
                        }
                    )
                }
        )

        playerError?.let { msg ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cy.OverlaySoft)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(msg, color = Cy.Text, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Coba Lagi",
                    color = Cy.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Cy.RadiusChip))
                        .background(Cy.Accent)
                        .clickable { retryPlayback() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        if (spin && playerError == null) {
            CircularProgressIndicator(
                color = Cy.Text2, strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(36.dp)
            )
        }

        skipFlash?.let { flash ->
            Box(
                Modifier
                    .align(if (flash.startsWith("-")) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Cy.OverlaySoft)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(flash, color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (controlsVisible) {
            // Gradasi atas/bawah ala web
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color(0xE0212237), Color.Transparent)))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE0212237))))
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Baris 1: Prev / -10 / Play / +10 / Next (slot seragam 48dp, simetris)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallPlayerButton(
                        Icons.Filled.SkipPrevious, enabled = prevId != null
                    ) { nav.navigate("watch/$type/$prevId") { popUpTo("watch/$type/$id") { inclusive = true } } }
                    SmallPlayerButton(Icons.Filled.Replay10) { skip(-10) }
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Cy.Text)
                            .clickable { toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (playing) "Jeda" else "Putar",
                            tint = Cy.Navy, modifier = Modifier.size(30.dp)
                        )
                    }
                    SmallPlayerButton(Icons.Filled.Forward10) { skip(10) }
                    SmallPlayerButton(
                        Icons.Filled.SkipNext, enabled = nextId != null
                    ) { nav.navigate("watch/$type/$nextId") { popUpTo("watch/$type/$id") { inclusive = true } } }
                }
                Spacer(Modifier.height(6.dp))
                // Baris 2: waktu (kiri) + kualitas / kecepatan / layar penuh (kanan), tinggi seragam 36dp
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${fmtTime(current)} / ${fmtTime(duration)}", color = Cy.Text, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PillButton(qualityLabel) { onOpenSettings() }
                        PillButton(
                            if (speed % 1.0f == 0f) "${speed.toInt()}x" else "${"%.2f".format(speed).trimEnd('0').trimEnd('.')}x"
                        ) {
                            speed = when (speed) {
                                0.5f -> 0.75f; 0.75f -> 1.0f; 1.0f -> 1.25f
                                1.25f -> 1.5f; 1.5f -> 2.0f; else -> 0.5f
                            }
                        }
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(Cy.RadiusChip))
                                .background(Color(0x1A_FEFDFF))
                                .clickable { isFullscreen = !isFullscreen; onFullscreen() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                "Layar penuh", tint = Cy.Text, modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                SeekBar(
                    progress = current,
                    duration = duration,
                    buffered = buffered
                ) { pos ->
                    player.seekTo((pos * 1000).toLong())
                    current = pos
                    controlsShownAt = System.currentTimeMillis()
                }
            }
        }
    }
}

@Composable
private fun SmallPlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            tint = if (enabled) Cy.Text else Color(0x4D_FEFDFF),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(Cy.RadiusChip))
            .background(Color(0x1A_FEFDFF))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Cy.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Seekbar tipis 3dp glacier + thumb 14dp, drag/tap untuk seek. */
@Composable
private fun SeekBar(
    progress: Double,
    duration: Double,
    buffered: Double,
    onSeek: (Double) -> Unit
) {
    val fraction = if (duration > 0) (progress / duration).coerceIn(0.0, 1.0) else 0.0
    val bufferedFraction = if (duration > 0) (buffered / duration).coerceIn(0.0, 1.0) else 0.0
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableDoubleStateOf(0.0) }
    val shown = if (dragging) dragFraction else fraction
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        dragging = true
                        dragFraction = (change.position.x / size.width).toDouble().coerceIn(0.0, 1.0)
                    },
                    onDragEnd = {
                        onSeek(dragFraction * duration)
                        dragging = false
                    },
                    onDragCancel = { dragging = false }
                )
            }
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).toDouble().coerceIn(0.0, 1.0)
                    onSeek(f * duration)
                }
            }
    ) {
        val w = maxWidth
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(w)
                .height(3.dp)
                .background(Color(0x40_FEFDFF))
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(w * bufferedFraction.toFloat())
                .height(3.dp)
                .background(Color(0x66_FEFDFF))
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(w * shown.toFloat())
                .height(3.dp)
                .background(Cy.Text2)
        )
        val thumbX = (w * shown.toFloat() - 7.dp).coerceAtLeast(0.dp)
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbX)
                .size(14.dp)
                .background(Cy.Text, RoundedCornerShape(50))
        )
    }
}

private fun fmtTime(s: Double): String {
    if (s.isNaN() || s <= 0) return "00:00"
    val total = s.toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
    else "%02d:%02d".format(m, sec)
}

/* ---------- Embed player (WebView, padanan iframe web) ---------- */

@Composable
private fun EmbedPlayer(url: String, onLoaded: () -> Unit = {}, onFatal: () -> Unit = {}) {
    // WebView dibuat sekali per halaman; saat halaman ditinggalkan WebView
    // di-pause & di-destroy supaya audio/video tidak lanjut di background
    // dan memorinya tidak bocor (padanan iframe yang ter-unmount di web).
    val context = LocalContext.current
    val webView = remember { WebView(context) }
    DisposableEffect(Unit) {
        webView.apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = false
            settings.useWideViewPort = true
            // Video blob/MSE (Mega) kadang hitam bila layer tak hardware.
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // UA Chrome seluler biasa (tanpa token "wv") -> host embed (desustream,
            // vidhide, mega) tidak menolak WebView sebagai bot.
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            android.webkit.CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@apply.let { webView }, true)
            }
            setBackgroundColor(android.graphics.Color.BLACK)
            // Fullscreen video dari dalam embed (tombol layar penuh player mereka)
            webChromeClient = object : android.webkit.WebChromeClient() {
                private var customView: android.view.View? = null
                override fun onShowCustomView(v: android.view.View?, cb: CustomViewCallback?) {
                    customView = v
                    (webView.parent as? android.view.ViewGroup)?.addView(v)
                }
                override fun onHideCustomView() {
                    (customView?.parent as? android.view.ViewGroup)?.removeView(customView)
                    customView = null
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, u: String?) { onLoaded() }
                // Halaman utama gagal dimuat (host mati/blokir) -> anggap server
                // gagal supaya otomatis pindah ke server lain.
                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) onFatal()
                }
                // Iklan/redirect pop-under: tahan navigasi ke luar host embed awal.
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: android.webkit.WebResourceRequest?
                ): Boolean {
                    val target = request?.url?.host ?: return false
                    val origin = android.net.Uri.parse(url).host ?: return false
                    return request.isForMainFrame && !target.endsWith(origin.substringAfter('.'))
                }
            }
            loadUrl(url)
        }
        onDispose {
            webView.apply {
                stopLoading()
                onPause()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }
    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize()
    )
}

