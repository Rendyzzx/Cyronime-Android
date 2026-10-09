package id.my.id.cyronime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.my.id.cyronime.app.data.AnimeItem
import id.my.id.cyronime.app.data.DonghuaItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Search meniru /search web persis: SearchBox (debounce 500ms), heading
 * "Hasil pencarian: {q}" dengan query berwarna aksen + jumlah hasil, lalu
 * grid poster 3 kolom per kategori (AnimeCard / DonghuaCard). Data dari
 * /api/search yang kini mengirim field kartu lengkap.
 */
@Composable
fun SearchScreen(nav: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<HomeApi.SearchFull?>(null) }
    var searchedFor by remember { mutableStateOf<String?>(null) }

    val io = rememberIoScope()

    fun doSearch() {
        val q = query.trim()
        if (q.length < 2) return
        loading = true
        error = null
        io.launch {
            try {
                results = HomeApi.searchFull(q)
                searchedFor = q
                loading = false
            } catch (err: Exception) {
                loading = false
                error = errorMessage(err)
            }
        }
    }

    // Debounce 500ms ala SearchBox.tsx
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = null
            searchedFor = null
            return@LaunchedEffect
        }
        delay(500)
        if (q == query.trim()) doSearch()
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Spacer(Modifier.height(16.dp))
                SearchBarField(
                    value = query,
                    onValueChange = { query = it },
                    onSubmit = { doSearch() },
                    placeholder = "Cari anime"
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        val r = results
        val q = searchedFor
        when {
            loading -> item {
                Text(
                    "Mencari…", color = Cy.Text2, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            error != null -> item {
                Text(
                    error!!, color = Cy.Text2, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            r != null && q != null -> {
                // Heading ala web: "Hasil pencarian: {q}" + jumlah hasil
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            buildAnnotatedString {
                                append("Hasil pencarian: ")
                                withStyle(SpanStyle(color = Cy.Accent, fontWeight = FontWeight.Bold)) {
                                    append(q)
                                }
                            },
                            color = Cy.Text,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${r.anime.size} anime, ${r.donghua.size} donghua",
                            color = Cy.Text2, fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                // Section Anime — grid 3 kolom ala AnimeCard
                item { SectionTitle("Anime") }
                if (r.anime.isEmpty()) {
                    item { EmptyPanel("Tidak ada anime yang cocok.") }
                } else {
                    items(r.anime.chunked(3), key = { it.first().animeId }) { row ->
                        PosterGridRow(
                            count = row.size,
                            content = { i ->
                                val a = row[i]
                                PosterCard(
                                    poster = a.poster,
                                    title = a.title,
                                    score = a.score,
                                    bottomChip = a.episodes?.let { "Eps $it" },
                                    onClick = { nav.navigate("detail/anime/${a.animeId}") }
                                )
                            }
                        )
                    }
                }
                // Section Donghua — grid 3 kolom ala DonghuaCard
                item { SectionTitle("Donghua") }
                if (r.donghua.isEmpty()) {
                    item { EmptyPanel("Tidak ada donghua yang cocok.") }
                } else {
                    items(r.donghua.chunked(3), key = { it.first().slug }) { row ->
                        PosterGridRow(
                            count = row.size,
                            content = { i ->
                                val d = row[i]
                                PosterCard(
                                    poster = d.poster,
                                    title = d.title,
                                    bottomChip = d.currentEpisode,
                                    onClick = { nav.navigate("detail/donghua/${d.slug}") }
                                )
                            }
                        )
                    }
                }
            }
            else -> item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Pencarian", color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ketik minimal 2 karakter pada kolom pencarian di atas.",
                        color = Cy.Text2, fontSize = 14.sp
                    )
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** Satu baris grid 3 kolom dengan gap 12dp ala grid-cols-3 gap-3 web. */
@Composable
private fun PosterGridRow(count: Int, content: @Composable (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) { i ->
            Box(Modifier.weight(1f)) { content(i) }
        }
        repeat(3 - count) { Spacer(Modifier.weight(1f)) }
    }
}

/** Panel kosong ala web: rounded-app, --surface, teks muted. */
@Composable
private fun EmptyPanel(message: String) {
    Text(
        message,
        color = Cy.Text2, fontSize = 14.sp,
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Cy.RadiusApp))
            .background(Cy.Surface)
            .padding(16.dp)
    )
}
