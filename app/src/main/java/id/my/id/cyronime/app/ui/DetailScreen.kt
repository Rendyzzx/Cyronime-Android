package id.my.id.cyronime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import id.my.id.cyronime.app.data.AnimeDetail
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.DonghuaDetail
import kotlinx.coroutines.launch

/**
 * Detail ala /anime/[slug] web: hero full-bleed 400dp dengan gradasi ke bg,
 * chip status + judul 32sp + chip info + genre chip + sinopsis expandable +
 * tombol Mulai nonton & Favorite + daftar episode (terbaru dulu) + rekomendasi.
 */
@Composable
fun DetailScreen(nav: NavController, type: String, slug: String) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var animeDetail by remember { mutableStateOf<AnimeDetail?>(null) }
    var donghuaDetail by remember { mutableStateOf<DonghuaDetail?>(null) }
    var favorite by remember { mutableStateOf(false) }

    val io = rememberIoScope()

    // Tampilkan data tersimpan seketika (tanpa layar "Memuat…"), lalu refresh senyap.
    var primed by remember { mutableStateOf(false) }
    if (!primed) {
        primed = true
        if (type == "anime") Api.peekAnimeDetail(slug)?.let { animeDetail = it; loading = false }
        else Api.peekDonghuaDetail(slug)?.let { donghuaDetail = it; loading = false }
    }

    fun reload() {
        if (animeDetail == null && donghuaDetail == null) loading = true
        io.launch {
            try {
                if (type == "anime") {
                    val d = Api.animeDetail(slug)
                    animeDetail = d
                    favorite = Api.favorites("anime").any { it.contentId == d.animeId }
                } else {
                    val d = Api.donghuaDetail(slug)
                    donghuaDetail = d
                    favorite = Api.favorites("donghua").any { it.contentId == d.slug }
                }
                error = null
            } catch (err: Exception) {
                error = errorMessage(err)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    fun toggleFavorite() {
        val d = animeDetail
        val dd = donghuaDetail
        val id = d?.animeId ?: dd?.slug ?: return
        val title = d?.title ?: dd?.title ?: ""
        val poster = d?.poster ?: dd?.poster ?: ""
        val next = !favorite
        io.launch {
            try {
                if (next) Api.addFavorite(type, id, title, poster)
                else Api.removeFavorite(type, id)
                favorite = next
            } catch (_: Exception) {
            }
        }
    }

    if (loading) {
        LoadingScreen()
        return
    }

    error?.let { message ->
        ErrorScreen(message, retry = { reload() })
        return
    }

    val title = animeDetail?.title ?: donghuaDetail?.title ?: ""
    val poster = animeDetail?.poster ?: donghuaDetail?.poster ?: ""
    val synopsis = animeDetail?.synopsis ?: donghuaDetail?.synopsis
    val status = animeDetail?.status ?: donghuaDetail?.status
    val score = animeDetail?.score ?: donghuaDetail?.rating
    val genres = animeDetail?.genres ?: donghuaDetail?.genres ?: emptyList()

    // 6 item: dengan related satu franchise (season/OVA/film) kini relevan
    // untuk ditampilkan lebih banyak, bukan hanya 4 rekomendasi generik.
    val recRows = remember(animeDetail) { animeDetail?.recommended?.take(6)?.chunked(2) ?: emptyList() }

    LazyColumn(Modifier.fillMaxSize()) {
        // ===== Hero (banner dengan jarak & sudut membulat) =====
        item {
            Box(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(Cy.RadiusCard))
                    .background(Cy.Surface)
            ) {
                AsyncImage(
                    model = absPoster(poster),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradasi bawah halus agar banner menyatu dengan konten
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.55f to androidx.compose.ui.graphics.Color.Transparent,
                                1f to androidx.compose.ui.graphics.Color(0xCC212237)
                            )
                        )
                )
                // Tombol back di atas hero (40dp, overlay-soft)
                Box(
                    Modifier
                        .padding(10.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Cy.OverlaySoft)
                        .clickable { nav.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Kembali", "Back"), tint = Cy.OnMedia, modifier = Modifier.size(32.dp))
                }
            }
        }

        // ===== Konten =====
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                if (!status.isNullOrBlank()) {
                    Row(
                        Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(Cy.RadiusChip))
                            .background(Cy.Overlay)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = Cy.Peach, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(status, color = Cy.Peach, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text(title, color = Cy.Text, fontSize = 32.sp, fontWeight = FontWeight.Medium, lineHeight = 38.sp)
                Spacer(Modifier.height(12.dp))
                // Chip info: skor + jumlah episode
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!score.isNullOrBlank()) InfoChip(score, star = true)
                    val epCount = animeDetail?.episodeList?.size ?: donghuaDetail?.episodes?.size
                    if (epCount != null && epCount > 0) InfoChip("$epCount Episode")
                }
                Spacer(Modifier.height(12.dp))
                if (genres.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genres.take(3).forEach { g -> GenreChip(g) }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(tr("Sinopsis", "Synopsis"), color = Cy.Text, fontSize = 21.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(8.dp))
                if (!synopsis.isNullOrBlank()) {
                    SynopsisText(synopsis)
                } else {
                    Text(
                        tr("Sinopsis untuk judul ini belum tersedia dari sumber.", "No synopsis is available for this title yet."),
                        color = Cy.Text2, fontSize = 14.sp, lineHeight = 20.sp
                    )
                }
                Spacer(Modifier.height(20.dp))
                // Tombol Mulai nonton + Favorite (ala web)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val firstEp = animeDetail?.episodeList?.firstOrNull()?.episodeId
                        ?: donghuaDetail?.episodes?.firstOrNull()?.slug
                    if (firstEp != null) {
                        Row(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(Cy.RadiusChip))
                                .background(Cy.Accent)
                                .clickable { nav.navigate("watch/$type/$firstEp") }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Cy.Text, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                tr("Mulai nonton eps 1", "Start watching ep 1"),
                                color = Cy.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // FavoriteButton ala web: 44dp, surface-2, ikon + teks 16sp bold
                    Row(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(Cy.RadiusChip))
                            .background(Cy.Surface2)
                            .clickable { toggleFavorite() }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favorit",
                            tint = if (favorite) Cy.Peach else Cy.Text,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (favorite) tr("Favorit", "Favorited") else tr("Tambah favorit", "Add to favorites"),
                            color = Cy.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ===== Episode list (terbaru dulu) =====
        val episodes = animeDetail?.episodeList?.reversed() ?: donghuaDetail?.episodes?.reversed()
        if (!episodes.isNullOrEmpty()) {
            item { SectionTitle(tr("Episode (${episodes.size})", "Episodes (${episodes.size})")) }
            items(
                episodes,
                key = {
                    when (it) {
                        is id.my.id.cyronime.app.data.AnimeEpisodeRef -> it.episodeId
                        is id.my.id.cyronime.app.data.DonghuaEpisodeRef -> it.slug
                        else -> it.toString()
                    }
                }
            ) { ep ->
                val epTitle = when (ep) {
                    is id.my.id.cyronime.app.data.AnimeEpisodeRef -> ep.title
                    is id.my.id.cyronime.app.data.DonghuaEpisodeRef -> ep.title
                    else -> ""
                }
                val epId = when (ep) {
                    is id.my.id.cyronime.app.data.AnimeEpisodeRef -> ep.episodeId
                    is id.my.id.cyronime.app.data.DonghuaEpisodeRef -> ep.slug
                    else -> ""
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(Cy.RadiusChip))
                        .background(Cy.Surface)
                        .clickable { nav.navigate("watch/$type/$epId") }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        epTitle,
                        color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            item {
                Text(
                    tr("Daftar episode belum tersedia.", "Episode list is not available yet."),
                    color = Cy.Text2, fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // ===== Anime Terkait (grid 2 kolom, 6 item) =====
        val recommended = animeDetail?.recommended
        if (!recommended.isNullOrEmpty()) {
            item { SectionTitle("Anime Terkait") }
            items(recRows, key = { it.first().animeId }) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
