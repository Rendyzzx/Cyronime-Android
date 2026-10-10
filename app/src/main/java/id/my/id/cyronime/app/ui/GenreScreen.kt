package id.my.id.cyronime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.my.id.cyronime.app.AppSettings
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.AnimeItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Jelajah anime per genre: deretan chip genre + grid 3 kolom infinite scroll. */
@Composable
fun GenreScreen(nav: NavController, initialGenreId: String? = null) {
    var genres by remember { mutableStateOf<List<Api.Genre>>(emptyList()) }
    var selected by remember { mutableStateOf(initialGenreId) }
    var items by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var appending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val io = rememberIoScope()

    fun loadItems(reset: Boolean) {
        val g = selected ?: return
        val p = if (reset) 1 else page + 1
        if (reset) { loading = true; error = null } else appending = true
        io.launch {
            try {
                val res = Api.animeByGenre(g, p)
                items = if (reset) res.items else items + res.items.filter { n -> items.none { it.animeId == n.animeId } }
                hasNext = res.hasNextPage
                page = p
            } catch (e: Exception) {
                if (reset) error = errorMessage(e)
            } finally { loading = false; appending = false }
        }
    }

    LaunchedEffect(Unit) {
        try {
            genres = Api.animeGenres()
            if (selected == null) selected = genres.firstOrNull()?.id
            if (genres.isEmpty()) { error = tr("Daftar genre belum tersedia.", "Genre list is not available yet."); loading = false }
        } catch (e: Exception) { error = errorMessage(e); loading = false }
    }
    LaunchedEffect(selected) { if (selected != null) { items = emptyList(); loadItems(true) } }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasNext && !appending && !loading && last >= listState.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore }.distinctUntilChanged().collect { if (it) loadItems(false) }
    }
    val cols = AppSettings.gridColumns
    val rows = remember(items, cols) { items.chunked(cols) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable { nav.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Kembali", "Back"), tint = Cy.Text, modifier = Modifier.size(22.dp)) }
                    Text("Genre", color = Cy.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres, key = { it.id }) { g ->
                        val on = g.id == selected
                        Box(
                            Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (on) Cy.Accent else Cy.Surface2)
                                .clickable { if (!on) selected = g.id }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(g.title, color = Cy.Text, fontSize = 14.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        when {
            loading -> item { LoadingScreen() }
            error != null -> item { ErrorScreen(error!!, retry = { if (genres.isEmpty()) nav.popBackStack() else loadItems(true) }) }
            items.isEmpty() -> item {
                Text(tr("Belum ada anime untuk genre ini.", "No anime for this genre yet."), color = Cy.Text2, fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp))
            }
            else -> items(rows, key = { it.first().animeId }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { a ->
                        Box(Modifier.weight(1f)) {
                            PosterCard(
                                poster = a.poster, title = a.title, score = a.score,
                                bottomChip = a.episodes?.let { "Eps $it" },
                                onClick = { nav.navigate("detail/anime/${a.animeId}") }
                            )
                        }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
