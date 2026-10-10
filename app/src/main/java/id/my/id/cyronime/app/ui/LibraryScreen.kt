package id.my.id.cyronime.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import id.my.id.cyronime.app.BuildConfig
import id.my.id.cyronime.app.AppSettings
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.Favorite
import id.my.id.cyronime.app.data.HistoryEntry
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Library meniru dua halaman web: /history (riwayat tontonan dengan tombol
 * hapus) dan /favorites (grid favorit dipisah Anime & Donghua). Semua data
 * dari endpoint yang sama dengan web (login wajib).
 */
@Composable
fun LibraryScreen(nav: NavController, mode: String) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var animeFav by remember { mutableStateOf<List<Favorite>>(emptyList()) }
    var donghuaFav by remember { mutableStateOf<List<Favorite>>(emptyList()) }

    val io = rememberIoScope()

    fun reload() {
        loading = true
        io.launch {
            try {
                if (mode == "history") {
                    history = Api.history()
                } else {
                    animeFav = Api.favorites("anime")
                    donghuaFav = Api.favorites("donghua")
                }
                error = null
            } catch (err: Exception) {
                error = errorMessage(err)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(mode) { reload() }

    val cols = AppSettings.gridColumns
    val animeFavRows = remember(animeFav, cols) { animeFav.chunked(cols) }
    val donghuaFavRows = remember(donghuaFav, cols) { donghuaFav.chunked(cols) }

    when {
        loading -> Column(Modifier.fillMaxSize()) {
            LibraryHeader(nav, mode, showClear = false, onClear = {})
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = Cy.Accent)
            }
        }
        error != null -> Column(Modifier.fillMaxSize()) {
            LibraryHeader(nav, mode, showClear = false, onClear = {})
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ErrorScreen(error!!, retry = { reload() })
            }
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            item {
                LibraryHeader(
                    nav, mode,
                    showClear = mode == "history" && history.isNotEmpty(),
                    onClear = {
                        io.launch {
                            try {
                                deleteHistory()
                                history = emptyList()
                            } catch (_: Exception) {
                            }
                        }
                    }
                )
            }

            if (mode == "history") {
                if (history.isEmpty()) {
                    item {
                        EmptyPanel(tr("Belum ada riwayat tontonan. Mulai nonton episode untuk mengisi history-mu.", "No watch history yet. Start watching an episode to fill it in."))
                    }
                } else {
                    items(history, key = { "${it.type}-${it.contentId}" }) { h ->
                        HistoryRow(h) { nav.navigate("detail/${h.type}/${h.contentId}") }
                    }
                }
            } else {
                item { SectionTitle("Anime") }
                if (animeFav.isEmpty()) {
                    item { EmptyPanel(tr("Belum ada anime favorit. Tambahkan lewat tombol ♥ di halaman detail anime.", "No favorite anime yet. Add some with the ♥ button on an anime page.")) }
                } else {
                    items(animeFavRows, key = { it.first().contentId }) { row ->
                        FavoriteRow(row) { f -> nav.navigate("detail/anime/${f.contentId}") }
                    }
                }
                item { SectionTitle("Donghua") }
                if (donghuaFav.isEmpty()) {
                    item { EmptyPanel(tr("Belum ada donghua favorit. Tambahkan lewat tombol ♥ di halaman detail donghua.", "No favorite donghua yet. Add some with the ♥ button on a donghua page.")) }
                } else {
                    items(donghuaFavRows, key = { it.first().contentId }) { row ->
                        FavoriteRow(row) { f -> nav.navigate("detail/donghua/${f.contentId}") }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/** DELETE /api/history — endpoint yang sama dengan ClearHistoryButton web. */
private suspend fun deleteHistory() {
    kotlinx.coroutines.withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(Api.base + "/api/history")
            .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
            .delete()
            .build()
        Api.client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw HttpErrorLite(res.code)
        }
    }
}

private class HttpErrorLite(val code: Int) : Exception()

@Composable
private fun LibraryHeader(nav: NavController, mode: String, showClear: Boolean, onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable { nav.popBackStack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Kembali", "Back"), tint = Cy.Text, modifier = Modifier.size(22.dp))
        }
        Text(
            if (mode == "history") tr("Riwayat Tontonan", "Watch History") else tr("Favorit", "Favorites"),
            color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (showClear) {
            Text(
                tr("Hapus", "Clear"),
                color = Cy.Peach, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onClear() }
            )
        }
    }
}

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

/** Baris riwayat ala /history web: poster 44x64, judul, "Episode N · tanggal", chevron. */
@Composable
private fun HistoryRow(h: HistoryEntry, onClick: () -> Unit) {
    val date = remember(h.watchedAt) {
        if (h.watchedAt <= 0) ""
        else SimpleDateFormat("d MMM", Locale("id", "ID")).format(Date(h.watchedAt))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = absPoster(h.poster),
            contentDescription = h.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 44.dp, height = 64.dp)
                .clip(RoundedCornerShape(Cy.RadiusApp))
                .background(Cy.Surface2)
        )
        Column(Modifier.weight(1f)) {
            Text(
                h.title, color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(if (h.episode != null) "Episode ${h.episode}" else if (h.type == "anime") "Anime" else "Donghua")
                    if (date.isNotBlank()) append(" · $date")
                },
                color = Cy.Text2, fontSize = 12.sp
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = Cy.Text2, modifier = Modifier.size(22.dp)
        )
    }
    Box(
        Modifier
            .padding(start = 72.dp)
            .height(1.dp)
            .fillMaxWidth()
            .background(Cy.Line)
    )
}

/** Baris grid favorit (3 kolom) ala FavoriteCard web. */
@Composable
private fun FavoriteRow(favorites: List<Favorite>, onClick: (Favorite) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        favorites.forEach { f ->
            Box(Modifier.weight(1f)) {
                PosterCard(
                    poster = f.poster,
                    title = f.title.ifBlank { f.contentId },
                    onClick = { onClick(f) }
                )
            }
        }
        repeat(AppSettings.gridColumns - favorites.size) { Spacer(Modifier.weight(1f)) }
    }
}
