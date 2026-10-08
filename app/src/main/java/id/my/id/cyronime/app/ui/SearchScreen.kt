package id.my.id.cyronime.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.SearchResults

/** Search: hasil gabungan Anime & Donghua dari /api/search (backend proxy). */
@Composable
fun SearchScreen(nav: NavController) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<SearchResults?>(null) }

    fun doSearch() {
        val q = query.trim()
        if (q.length < 2) return
        loading = true
        error = null
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                results = Api.search(q)
                loading = false
            } catch (err: Exception) {
                loading = false
                error = errorMessage(err)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari anime atau donghua…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        when {
            loading -> LoadingScreen("Mencari…")
            error != null -> ErrorScreen(error!!, retry = { doSearch() })
            results != null -> {
                val r = results!!
                LazyColumn {
                    if (r.anime.isNotEmpty()) {
                        item { SectionTitle("Anime") }
                        items(r.anime) { (title, animeId) ->
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { nav.navigate("detail/anime/$animeId") }
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    }
                    if (r.donghua.isNotEmpty()) {
                        item { SectionTitle("Donghua") }
                        items(r.donghua) { (title, slug) ->
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { nav.navigate("detail/donghua/$slug") }
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    }
                    if (r.anime.isEmpty() && r.donghua.isEmpty()) {
                        item {
                            Text(
                                "Tidak ada hasil untuk \"${query}\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
