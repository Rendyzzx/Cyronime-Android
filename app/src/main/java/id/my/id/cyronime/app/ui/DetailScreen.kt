package id.my.id.cyronime.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import id.my.id.cyronime.app.data.AnimeDetail
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.DonghuaDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Detail: Anime (GET /api/anime/{slug}) atau Donghua (GET /api/donghua/{slug}). */
@Composable
fun DetailScreen(nav: NavController, type: String, slug: String) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var animeDetail by remember { mutableStateOf<AnimeDetail?>(null) }
    var donghuaDetail by remember { mutableStateOf<DonghuaDetail?>(null) }
    var favorite by remember { mutableStateOf(false) }

    fun reload() {
        loading = true
        CoroutineScope(Dispatchers.IO).launch {
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (next) {
                    Api.addFavorite(type, id, title, poster)
                } else {
                    Api.removeFavorite(type, id)
                }
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
    val genres = animeDetail?.genres ?: donghuaDetail?.genres ?: emptyList()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { toggleFavorite() }) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (favorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp)) {
                AsyncImage(
                    model = absPoster(poster),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(120.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    if (status != null) Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
                    animeDetail?.score?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Skor: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    donghuaDetail?.rating?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Rating: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        genres.take(3).forEach { g ->
                            AssistChip(onClick = {}, label = { Text(g, maxLines = 1) })
                        }
                    }
                }
            }
        }
        if (!synopsis.isNullOrBlank()) {
            item {
                SectionTitle("Sinopsis")
                Text(
                    synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        item { SectionTitle("Episode") }
        val episodes = animeDetail?.episodeList
        if (episodes != null) {
            items(episodes.reversed()) { ep ->
                Text(
                    ep.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("watch/anime/${ep.episodeId}") }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
        donghuaDetail?.episodes?.reversed()?.let { eps ->
            items(eps) { ep ->
                Text(
                    ep.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("watch/donghua/${ep.slug}") }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
