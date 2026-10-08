package id.my.id.cyronime.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.Favorite
import id.my.id.cyronime.app.data.HistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Library: History + Favorites — data akun yang sama dengan Web
 * (GET /api/history & /api/favorites, login wajib).
 */
@Composable
fun LibraryScreen(nav: NavController) {
    var tab by remember { mutableIntStateOf(0) } // 0 = History, 1 = Favorit
    var favType by remember { mutableStateOf("anime") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var favorites by remember { mutableStateOf<List<Favorite>>(emptyList()) }

    fun reload() {
        loading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (tab == 0) {
                    history = Api.history()
                } else {
                    favorites = Api.favorites(favType)
                }
                error = null
            } catch (err: Exception) {
                error = errorMessage(err)
            } finally {
                loading = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(tab, favType) { reload() }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("History") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Favorit") })
        }
        if (tab == 1) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(
                    selected = favType == "anime",
                    onClick = { favType = "anime" },
                    label = { Text("Anime") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = favType == "donghua",
                    onClick = { favType = "donghua" },
                    label = { Text("Donghua") }
                )
            }
        }

        when {
            loading -> LoadingScreen()
            error != null -> ErrorScreen(error!!, retry = { reload() })
            else -> {
                val list = if (tab == 0) history.map { it.title to it.poster } else favorites.map { it.title to it.poster }
                if (list.isEmpty()) {
                    Text(
                        if (tab == 0) "Belum ada riwayat tontonan."
                        else "Belum ada favorit. Tandai lewat tombol hati di halaman detail.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn {
                        items(
                            if (tab == 0) history else favorites,
                            key = { if (tab == 0) (it as HistoryEntry).contentId + (it as HistoryEntry).type else (it as Favorite).contentId }
                        ) { item ->
                            val (title, poster, contentId, type) = when (item) {
                                is HistoryEntry -> listOf(item.title, item.poster, item.contentId, item.type)
                                is Favorite -> listOf(item.title, item.poster, item.contentId, favType)
                                else -> listOf("", "", "", "anime")
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { nav.navigate("detail/$type/$contentId") }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                AsyncImage(
                                    model = absPoster(poster),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(width = 52.dp, height = 72.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.padding(top = 4.dp)) {
                                    Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (item is HistoryEntry && item.episode != null) {
                                        Text(
                                            "Episode ${item.episode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}
