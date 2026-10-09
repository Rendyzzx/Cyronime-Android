package id.my.id.cyronime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.HttpError
import id.my.id.cyronime.app.data.SystemStatus
import java.io.IOException

/** Poster absolut (relative dari backend di-prefix base URL). */
fun absPoster(poster: String?): String? =
    if (poster.isNullOrBlank()) null
    else if (poster.startsWith("http")) poster
    else Api.base + poster

/* ---------- scope IO yang mengikuti lifecycle halaman ---------- */

/**
 * Scope IO yang mengikuti lifecycle composable: semua request dibatalkan
 * otomatis saat halaman ditinggalkan, sehingga tidak ada request yang terus
 * berjalan di background (hemat kuota, mencegah state tertimpa, tidak
 * menumpuk saat retry ditekan berulang).
 */
@Composable
fun rememberIoScope(): CoroutineScope {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    DisposableEffect(Unit) {
        onDispose { scope.cancel() }
    }
    return scope
}

/* ---------- state umum ---------- */

@Composable
fun LoadingScreen(text: String = "Memuat…") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorScreen(message: String, retry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            if (retry != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = retry) { Text("Coba lagi") }
            }
        }
    }
}

/** Pesan error berdasarkan jenis exception — offline vs server vs HTTP. */
fun errorMessage(err: Exception): String = when (err) {
    is IOException -> "Tidak ada koneksi internet. Periksa jaringan Anda lalu coba lagi."
    is HttpError -> if (err.code == 401) "Sesi berakhir. Silakan login ulang."
    else "Server Cyronime sedang tidak dapat diakses. Coba lagi beberapa saat."
    else -> "Terjadi kesalahan. Coba lagi."
}

/* ---------- kartu poster (meniru AnimeCard.tsx / DonghuaCard.tsx) ---------- */

/** Brush konstan: dibuat sekali di level file, tidak dialokasi ulang per kartu. */
private val PosterGradient = Brush.verticalGradient(
    listOf(Color(0x00_000000), Color(0xB3_000000))
)

/**
 * Poster 3:4 radius 18, chip rating kanan-atas (bintang peach), chip kiri-bawah
 * (rounded-md 12sp), judul 14sp semibold 2 baris mt-2. Digunakan untuk kartu
 * rail (lebar 128dp) dan grid (weight 1f).
 */
@Composable
fun PosterCard(
    poster: String?,
    title: String,
    onClick: () -> Unit,
    score: String? = null,
    bottomChip: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(Cy.RadiusCard))
                .background(Cy.Surface)
        ) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val url = absPoster(poster)
            // Request di-remember + ukuran dibatasi (poster kartu ~360px cukup):
            // decode gambar kecil = scroll jauh lebih ringan & hemat RAM.
            val request = remember(url) {
                coil.request.ImageRequest.Builder(ctx)
                    .data(url)
                    .size(360, 480)
                    .crossfade(false)
                    .allowRgb565(true)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradasi bawah ala web: Brush dibuat sekali (bukan tiap recomposition)
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(64.dp)
                    .background(PosterGradient)
            )
            if (!score.isNullOrBlank()) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(Cy.RadiusChip))
                        .background(Cy.Overlay)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, tint = Cy.Peach, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(score, color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (!bottomChip.isNullOrBlank()) {
                Text(
                    bottomChip,
                    color = Cy.Text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .widthIn(max = 150.dp)
                        .clip(RoundedCornerShape(Cy.RadiusMd))
                        .background(Cy.Overlay)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Text(
            title,
            color = Cy.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Header section ala SectionHeader.tsx: judul 17sp kiri, link peach kanan. */
@Composable
fun SectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text, color = Cy.Text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        if (action != null && onAction != null) {
            Text(
                action,
                color = Cy.Peach,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

/** Chip portal ala PortalSwitch.tsx (Anime / Donghua) — tinggi 44, radius 8. */
@Composable
fun PortalChip(isAnime: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Cy.RadiusChip))
            .background(Cy.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isAnime) Icons.Filled.LiveTv else Icons.Filled.AutoAwesome,
            null, tint = Cy.Text2, modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(if (isAnime) "Anime" else "Donghua", color = Cy.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.SwapVert, null, tint = Cy.Text2, modifier = Modifier.size(18.dp))
    }
}

/* ---------- komponen ala halaman detail web ---------- */

/** Chip info 32dp (skor, status, jumlah episode) — bg surface-2 ala web. */
@Composable
fun InfoChip(text: String, star: Boolean = false) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(Cy.RadiusChip))
            .background(Cy.Surface2)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (star) {
            Icon(Icons.Filled.Star, null, tint = Cy.Peach, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Chip genre 32dp: border 1px --line di atas --surface (ala /anime/[slug]). */
@Composable
fun GenreChip(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text,
        color = Cy.Text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(Cy.RadiusChip))
            .background(Cy.Surface)
            .border(1.dp, Cy.Line, RoundedCornerShape(Cy.RadiusChip))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

/**
 * Sinopsis expandable ala Synopsis.tsx: teks 15sp muted, dipotong [lines]
 * baris, toggle "Selengkapnya"/"Lebih sedikit" dengan warna aksen.
 */
@Composable
fun SynopsisText(text: String, lines: Int = 5, accent: Color = Cy.Accent) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            text,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = Cy.Text2,
            maxLines = if (expanded) Int.MAX_VALUE else lines,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (expanded) "Lebih sedikit" else "Selengkapnya",
            color = accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { expanded = !expanded }
        )
    }
}

/**
 * Tombol hero ala .btn-hero: aksen datar, radius asimetris 16/20, ikon play
 * dalam lingkaran navy 28%, teks bold 19sp (kontras AA di atas --accent).
 */
@Composable
fun HeroButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 20.dp))
            .background(Cy.Accent)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50)
                )
                .background(Color(0x47_212237)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow, null,
                tint = Cy.Text, modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(label, color = Cy.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

/** Kotak ikon 44dp (radius-md, surface) — tombol profil/search di header. */
@Composable
fun SquareIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(Cy.RadiusMd))
            .background(Cy.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Cy.Text2, modifier = Modifier.size(22.dp))
    }
}

/* ---------- tab ala Tabs.tsx (listing anime) ---------- */

/** Container pill --surface p-1; tab aktif = --accent bg + teks putih. */
@Composable
fun TabsRow(tabs: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(Cy.RadiusApp))
            .background(Cy.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { i, (label, active) ->
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(Cy.RadiusChip))
                    .background(if (active) Cy.Accent else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) Cy.Text else Cy.Text2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ---------- search box ala SearchBox.tsx ---------- */

/** Kolom 48dp --surface-2 radius-chip, ikon kiri, placeholder "Cari anime". */
@Composable
fun SearchBarField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari anime"
) {
    // Fokus + keyboard: sebelumnya decorationBox TIDAK memanggil inner() saat
    // kosong, jadi field tak punya area input dan keyboard tak pernah muncul.
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(Cy.RadiusChip))
            .background(Cy.Surface2)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, "Cari", tint = Cy.Text2, modifier = Modifier.size(20.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Cy.Text, fontSize = 15.sp
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSubmit() }
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Cy.Accent),
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = Cy.Text2, fontSize = 15.sp)
                    }
                    inner()
                }
            }
        )
    }
}

/* ---------- layar maintenance & update (overlay penuh) ---------- */

@Composable
fun MaintenanceScreen(status: SystemStatus, retry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cyronime sedang dalam Maintenance", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            status.message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (status.estimatedEnd != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Perkiraan selesai: ${status.estimatedEnd}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = retry) { Text("Coba lagi") }
    }
}

/** Bandingkan dua string versi semantik ringan ("1.2.3"). true jika a < b. */
fun versionLessThan(a: String, b: String): Boolean {
    val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
    val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x != y) return x < y
    }
    return false
}

/** 15834723 -> "15,8 jt" — sama dengan formatViews() di web. */
fun formatViews(n: Long): String = when {
    n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0).replace(".", ",")} jt"
    n >= 1_000 -> "${n / 1_000} rb"
    else -> n.toString()
}
