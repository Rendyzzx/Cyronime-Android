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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

/* ---------- komponen kartu ---------- */

@Composable
fun PosterCard(
    poster: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                AsyncImage(
                    model = absPoster(poster),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(Modifier.padding(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
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
