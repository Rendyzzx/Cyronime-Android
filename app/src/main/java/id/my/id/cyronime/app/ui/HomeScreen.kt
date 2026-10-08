package id.my.id.cyronime.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import id.my.id.cyronime.app.data.AnimeItem
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.DonghuaItem
import id.my.id.cyronime.app.data.WatchProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home: Continue Watching (sinkron dengan Web) + Anime Ongoing + Donghua
 * Terbaru. Semua data dari backend — tidak ada request ke API sumber.
 */
@Composable
fun HomeScreen(nav: NavController) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<List<WatchProgress>>(emptyList()) }
    var anime by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var animeHasNext by remember { mutableStateOf(false) }
    var donghua by remember { mutableStateOf<List<DonghuaItem>>(emptyList()) }
    var animePage by remember { mutableStateOf(1) }

    fun load(reset: Boolean) {
        val page = if (reset) 1 else animePage + 1
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (reset) {
                    val p = Api.progressList()
                    val a = Api.animeList("ongoing", 1)
                    val d = Api.donghuaList("latest", 1)
                    progress = p
                    anime = a.items
                    animeHasNext = a.hasNextPage
                    donghua = d.items
                    animePage = 1
                    error = null
                } else {
                    val a = Api.animeList("ongoing", page)
                    anime = anime + a.items
                    animeHasNext = a.hasNextPage
                    animePage = page
                }
                loading = false
            } catch (err: Exception) {
                loading = false
                error = errorMessage(err)
            }
        }
    }

    LaunchedEffect(Unit) { load(reset = true) }

    when {
        loading -> LoadingScreen()
        error != null -> ErrorScreen(error!!, retry = {
            loading = true
            load(reset = true)
        })
        else -> LazyColumn(Modifier.fillMaxSize()) {
            if (progress.isNotEmpty()) {
                item {
                    SectionTitle("Lanjut Nonton")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(progress) { p ->
                            ContinueCard(p) {
                                nav.navigate("watch/${p.type}/${p.episodeId}")
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Anime Ongoing") }
            item {
                // baris manual — bukan grid nested, supaya scroll tunggal stabil
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    anime.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { a ->
                                Column(Modifier.weight(1f)) {
                                    PosterCard(
                                        poster = a.poster,
                                        title = a.title,
                                        subtitle = listOfNotNull(
                                            a.status,
                                            if (a.episodes != null) "${a.episodes} eps" else null
                                        ).joinToString(" • "),
                                        onClick = { nav.navigate("detail/anime/${a.animeId}") }
                                    )
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (animeHasNext) {
                item {
                    TextButton(
                        onClick = { load(reset = false) },
                        modifier = Modifier.padding(start = 16.dp)
                    ) { Text("Muat lebih banyak") }
                }
            }

            item { SectionTitle("Donghua Terbaru") }
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    donghua.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { d ->
                                Column(Modifier.weight(1f)) {
                                    PosterCard(
                                        poster = d.poster,
                                        title = d.title,
                                        subtitle = d.currentEpisode ?: d.status,
                                        onClick = { nav.navigate("detail/donghua/${d.slug}") }
                                    )
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ContinueCard(p: WatchProgress, onClick: () -> Unit) {
    Row(
        Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = absPoster(p.poster),
            contentDescription = p.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 72.dp, height = 100.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(p.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                if (p.episode != null) "Episode ${p.episode}" else "Lanjut menonton",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
