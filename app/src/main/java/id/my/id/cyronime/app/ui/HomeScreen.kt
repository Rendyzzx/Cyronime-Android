package id.my.id.cyronime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import id.my.id.cyronime.app.Prefs
import id.my.id.cyronime.app.data.AnimeItem
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.DonghuaItem
import id.my.id.cyronime.app.data.WatchProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Home = dashboard ala web ("/" setelah onboarding): header brand + portal
 * switch, search bar, HERO (lanjut nonton / unggulan portal), rail poster,
 * dan ranking Terpopuler. Semua data dari backend; portal aktif menentukan
 * isi (anime ATAU donghua) — sama seperti web.
 */
@Composable
fun HomeScreen(nav: NavController) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var isAnime by remember { mutableStateOf(Prefs.portal(ctx) == "anime") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<List<WatchProgress>>(emptyList()) }
    var animeOngoing by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var popular by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var donghuaLatest by remember { mutableStateOf<List<DonghuaItem>>(emptyList()) }
    var donghuaOngoing by remember { mutableStateOf<List<DonghuaItem>>(emptyList()) }

    fun load() {
        loading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val p = Api.progressList()
                progress = p
                if (isAnime) {
                    val ongoing = Api.animeList("ongoing", 1).items
                    animeOngoing = ongoing
                    // Terpopuler: padanan fallback web (completed diurut skor).
                    val completed = Api.animeList("completed", 1).items
                    popular = completed.sortedByDescending { it.score?.toDoubleOrNull() ?: 0.0 }.take(5)
                } else {
                    donghuaLatest = Api.donghuaList("latest", 1).items
                    donghuaOngoing = Api.donghuaList("ongoing", 1).items
                }
                error = null
            } catch (err: Exception) {
                error = errorMessage(err)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(isAnime) { load() }

    when {
        loading -> LoadingScreen()
        error != null -> ErrorScreen(error!!, retry = { load() })
        else -> LazyColumn(Modifier.fillMaxSize()) {
            // ===== Header: brand + portal switch + profil =====
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cyronime", color = Cy.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PortalChip(isAnime = isAnime, onClick = {
                            val next = !isAnime
                            isAnime = next
                            Prefs.setPortal(ctx, if (next) "anime" else "donghua")
                        })
                        SquareIconButton(Icons.Filled.Person, "Profil") { nav.navigate("profile") }
                    }
                }
            }
            // ===== Search bar (pindah ke layar search) =====
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(Cy.RadiusChip))
                        .background(Cy.Surface2)
                        .clickable { nav.navigate("search") }
                        .padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, "Cari", tint = Cy.Text2, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Cari anime", color = Cy.Text2, fontSize = 15.sp)
                }
                Spacer(Modifier.height(24.dp))
            }

            // ===== HERO =====
            val resume = progress.firstOrNull { (it.type == "anime") == isAnime }
            val featuredAnime = popular.firstOrNull() ?: animeOngoing.firstOrNull()
            val featuredDonghua = donghuaLatest.firstOrNull() ?: donghuaOngoing.firstOrNull()
            when {
                resume != null -> item { HeroResume(resume, nav) }
                isAnime && featuredAnime != null -> item {
                    HeroFeatured(
                        poster = featuredAnime.poster,
                        title = featuredAnime.title,
                        chip = "Sedang populer",
                        subtitle = buildString {
                            if (!featuredAnime.score.isNullOrBlank()) append("Skor ${featuredAnime.score}")
                            else append("Tonton sekarang")
                            featuredAnime.episodes?.let { append(", $it eps") }
                        },
                        onWatch = { nav.navigate("detail/anime/${featuredAnime.animeId}") }
                    )
                }
                !isAnime && featuredDonghua != null -> item {
                    HeroFeatured(
                        poster = featuredDonghua.poster,
                        title = featuredDonghua.title,
                        chip = "Donghua terbaru",
                        subtitle = listOfNotNull(
                            featuredDonghua.status, featuredDonghua.currentEpisode
                        ).joinToString(", "),
                        onWatch = { nav.navigate("detail/donghua/${featuredDonghua.slug}") }
                    )
                }
            }

            if (isAnime) {
                // ===== Anime Terbaru =====
                if (animeOngoing.isNotEmpty()) {
                    item {
                        RailHeader("Anime Terbaru", "Lihat semua") { nav.navigate("portal/anime") }
                        Text(
                            "Episode terbaru yang sedang tayang.",
                            color = Cy.Text2, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(animeOngoing.take(15), key = { it.animeId }) { a ->
                                Box(Modifier.width(128.dp)) {
                                    PosterCard(
                                        poster = a.poster, title = a.title,
                                        score = a.score,
                                        bottomChip = a.episodes?.let { "Eps $it" },
                                        onClick = { nav.navigate("detail/anime/${a.animeId}") }
                                    )
                                }
                            }
                        }
                    }
                }
                // ===== Terpopuler =====
                if (popular.isNotEmpty()) {
                    item {
                        Text(
                            "Terpopuler",
                            color = Cy.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 24.dp)
                        )
                        Text(
                            "Diurutkan dari skor tertinggi.",
                            color = Cy.Text2, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp)
                        )
                    }
                    itemsIndexed(popular, key = { _, it -> "pop-${it.animeId}" }) { i, a ->
                        RankRow(
                            rank = i + 1,
                            poster = a.poster,
                            title = a.title,
                            subtitle = buildString {
                                if (!a.score.isNullOrBlank()) append("Skor ${a.score}") else append("Populer")
                                a.episodes?.let { append(", $it eps") }
                            },
                            onClick = { nav.navigate("detail/anime/${a.animeId}") }
                        )
                    }
                }
            } else {
                // ===== Donghua Terbaru =====
                if (donghuaLatest.isNotEmpty()) {
                    item {
                        RailHeader("Donghua Terbaru", "Lihat semua") { nav.navigate("portal/donghua") }
                        Text(
                            "Rilisan donghua terbaru.",
                            color = Cy.Text2, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(donghuaLatest.take(15), key = { it.slug }) { d ->
                                Box(Modifier.width(128.dp)) {
                                    PosterCard(
                                        poster = d.poster, title = d.title,
                                        bottomChip = d.currentEpisode,
                                        onClick = { nav.navigate("detail/donghua/${d.slug}") }
                                    )
                                }
                            }
                        }
                    }
                }
                // ===== Donghua Ongoing =====
                if (donghuaOngoing.isNotEmpty()) {
                    item { RailHeader("Donghua Ongoing", null) {} }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(donghuaOngoing.take(15), key = { it.slug }) { d ->
                                Box(Modifier.width(128.dp)) {
                                    PosterCard(
                                        poster = d.poster, title = d.title,
                                        bottomChip = d.currentEpisode,
                                        onClick = { nav.navigate("detail/donghua/${d.slug}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/* ---------- HERO ala page.tsx web ---------- */

@Composable
private fun HeroBase(
    poster: String?,
    chip: String,
    title: String,
    subtitle: String,
    onWatch: () -> Unit,
    onInfo: (() -> Unit)? = null
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(330.dp)
            .clip(RoundedCornerShape(Cy.RadiusCard))
            .background(Cy.Surface)
    ) {
        AsyncImage(
            model = absPoster(poster),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Cahaya senja ala web: transparan -> 60% -> 96% navy
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(330.dp * 0.62f)
                .background(
                    Brush.verticalGradient(
                        listOf(Cy.HeroScrimTop, Cy.HeroScrimMid, Cy.HeroScrimBottom)
                    )
                )
        )
        Text(
            chip,
            color = Cy.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(Cy.RadiusChip))
                .background(Cy.Overlay)
                .border(1.dp, Cy.LineStrong, RoundedCornerShape(Cy.RadiusChip))
                .padding(horizontal = 14.dp, vertical = 7.dp)
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                title,
                color = Cy.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                lineHeight = 29.4.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Cy.Peach, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroButton("Tonton sekarang", onWatch)
                if (onInfo != null) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(Cy.RadiusMd))
                            .background(Cy.Overlay)
                            .border(1.dp, Cy.LineStrong, RoundedCornerShape(Cy.RadiusMd))
                            .clickable(onClick = onInfo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Info, "Detail", tint = Cy.Text2, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroResume(p: WatchProgress, nav: NavController) {
    HeroBase(
        poster = p.poster,
        chip = "Lanjut nonton",
        title = p.title,
        subtitle = if (p.episode != null) "Episode ${p.episode}" else "Lanjutkan dari terakhir kali",
        onWatch = { nav.navigate("watch/${p.type}/${p.episodeId}") },
        onInfo = { nav.navigate("detail/${p.type}/${p.contentId}") }
    )
}

@Composable
private fun HeroFeatured(
    poster: String?,
    title: String,
    chip: String,
    subtitle: String,
    onWatch: () -> Unit
) {
    HeroBase(poster, chip, title, subtitle, onWatch, onInfo = null)
}

/* ---------- rail & ranking ---------- */

@Composable
private fun RailHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, color = Cy.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (action != null) {
            Text(
                action,
                color = Cy.Peach, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

/** Baris ranking ala HomeRanking: nomor 38sp, poster 56x74, judul, chevron. */
@Composable
private fun RankRow(rank: Int, poster: String?, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(Cy.RadiusApp))
            .background(Cy.Surface)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "$rank",
            color = if (rank == 1) Cy.Peach else Cy.Text2,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(44.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        AsyncImage(
            model = absPoster(poster),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 56.dp, height = 74.dp)
                .clip(RoundedCornerShape(Cy.RadiusApp))
                .background(Cy.Surface2)
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Cy.Text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.Filled.ChevronRight, null,
            tint = Cy.Text2, modifier = Modifier.size(20.dp)
        )
    }
}

/* ============================================================
 * PortalListScreen — halaman /anime & /donghua web:
 * SearchBox, judul + PortalSwitch, tabs, grid poster infinite.
 * ============================================================ */

@Composable
fun PortalListScreen(nav: NavController, type: String) {
    val isAnime = type == "anime"
    var tab by remember { mutableStateOf(0) } // 0 = Sedang Tayang / Latest, 1 = Tamat / Ongoing
    var loading by remember { mutableStateOf(true) }
    var appending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var animeItems by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var donghuaItems by remember { mutableStateOf<List<DonghuaItem>>(emptyList()) }
    var hasNext by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberLazyListState()

    fun apiTab(): String = if (isAnime) (if (tab == 0) "ongoing" else "completed")
    else (if (tab == 0) "latest" else "ongoing")

    fun load(reset: Boolean) {
        val p = if (reset) 1 else page + 1
        if (reset) loading = true else appending = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isAnime) {
                    val res = Api.animeList(apiTab(), p)
                    animeItems = if (reset) res.items else animeItems + res.items
                    hasNext = res.hasNextPage
                } else {
                    val res = Api.donghuaList(apiTab(), p)
                    donghuaItems = if (reset) res.items else donghuaItems + res.items
                    hasNext = res.hasNextPage
                }
                page = p
                error = null
            } catch (err: Exception) {
                if (reset) error = errorMessage(err)
            } finally {
                loading = false
                appending = false
            }
        }
    }

    LaunchedEffect(type, tab) { load(reset = true) }

    // Infinite scroll: muat halaman berikutnya saat mendekati akhir.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasNext && !appending && last >= listState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }.distinctUntilChanged().collect {
            if (it) load(reset = false)
        }
    }

    when {
        loading -> LoadingScreen()
        error != null -> ErrorScreen(error!!, retry = { load(reset = true) })
        else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Column(Modifier.statusBarsPadding()) {
                    // SearchBox
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(Cy.RadiusChip))
                            .background(Cy.Surface2)
                            .clickable { nav.navigate("search") }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, "Cari", tint = Cy.Text2, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("Cari anime", color = Cy.Text2, fontSize = 15.sp)
                    }
                    // Judul + PortalSwitch
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isAnime) "Anime" else "Donghua",
                            color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        PortalChip(isAnime = isAnime, onClick = {
                            val next = if (isAnime) "donghua" else "anime"
                            Prefs.setPortal(ctx, next)
                            nav.navigate("home") { popUpTo("home") { inclusive = true } }
                        })
                    }
                    Spacer(Modifier.height(20.dp))
                    TabsRow(
                        tabs = listOf(
                            (if (isAnime) "Sedang Tayang" else "Terbaru") to (tab == 0),
                            (if (isAnime) "Tamat" else "Ongoing") to (tab == 1)
                        )
                    ) { tab = it }
                    Spacer(Modifier.height(20.dp))
                }
            }
            // Grid 3 kolom ala InfiniteGrid web (grid-cols-3 gap-3)
            if (isAnime) {
                items(animeItems.chunked(3), key = { it.first().animeId }) { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { a ->
                            Box(Modifier.weight(1f)) {
                                PosterCard(
                                    poster = a.poster, title = a.title,
                                    score = a.score,
                                    bottomChip = a.episodes?.let { "Eps $it" },
                                    onClick = { nav.navigate("detail/anime/${a.animeId}") }
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else {
                items(donghuaItems.chunked(3), key = { it.first().slug }) { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { d ->
                            Box(Modifier.weight(1f)) {
                                PosterCard(
                                    poster = d.poster, title = d.title,
                                    bottomChip = d.currentEpisode,
                                    onClick = { nav.navigate("detail/donghua/${d.slug}") }
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (appending) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Cy.Accent, modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
