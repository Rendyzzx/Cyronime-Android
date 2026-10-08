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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.SearchResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Search ala /search web: search box dengan debounce 500ms, hasil dipisah
 * Anime / Donghua. Kontrak /api/search Android hanya mengirim title + id
 * (tanpa poster), jadi hasil dirender baris teks ala dropdown SearchBox.
 */
@Composable
fun SearchScreen(nav: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<SearchResults?>(null) }
    var searchedFor by remember { mutableStateOf<String?>(null) }

    fun doSearch() {
        val q = query.trim()
        if (q.length < 2) return
        loading = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                results = Api.search(q)
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
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Hasil pencarian: $q", color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${r.anime.size} anime, ${r.donghua.size} donghua",
                            color = Cy.Text2, fontSize = 14.sp
                        )
                    }
                }
                if (r.anime.isNotEmpty()) {
                    item { SectionTitle("Anime") }
                    items(r.anime, key = { "a-${it.second}" }) { (title, animeId) ->
                        ResultRow(title) { nav.navigate("detail/anime/$animeId") }
                    }
                } else {
                    item { SectionTitle("Anime") }
                    item { EmptyNote("Tidak ada anime yang cocok.") }
                }
                if (r.donghua.isNotEmpty()) {
                    item { SectionTitle("Donghua") }
                    items(r.donghua, key = { "d-${it.second}" }) { (title, slug) ->
                        ResultRow(title) { nav.navigate("detail/donghua/$slug") }
                    }
                } else {
                    item { SectionTitle("Donghua") }
                    item { EmptyNote("Tidak ada donghua yang cocok.") }
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

/** Baris hasil ala dropdown SearchBox: baris teks truncate di panel surface-2. */
@Composable
private fun ResultRow(title: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Cy.RadiusApp))
            .background(Cy.Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            title, color = Cy.Text, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyNote(message: String) {
    Text(
        message,
        color = Cy.Text2, fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}
