package id.my.id.cyronime.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.text.style.TextAlign
import id.my.id.cyronime.app.AppSettings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.messaging.FirebaseMessaging
import id.my.id.cyronime.app.BuildConfig
import id.my.id.cyronime.app.Prefs
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.HttpError
import id.my.id.cyronime.app.data.Me
import id.my.id.cyronime.app.data.NotifyPrefs
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Profile ala /profile web: kartu akun (avatar 56dp + nama + email), daftar
 * tautan (Watch History, Favorites, Settings), dan tombol logout.
 */
@Composable
fun ProfileScreen(nav: NavController, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    var me by remember { mutableStateOf<Me?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            me = Api.me()
        } catch (_: Exception) {
            me = null
        } finally {
            loading = false
        }
    }

    val io = rememberIoScope()

    fun logout() {
        io.launch {
            SettingsActions.logout(context)
            kotlinx.coroutines.withContext(Dispatchers.Main) { onLoggedOut() }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(tr("Profil", "Profile"), color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().height(88.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cy.Accent, modifier = Modifier.size(28.dp))
            }
        } else {
            // Kartu akun ala web: surface radius-card, avatar 56, nama 17sp bold
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Cy.RadiusCard))
                    .background(Cy.Surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!me?.image.isNullOrBlank()) {
                    AsyncImage(
                        model = me!!.image,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Cy.Surface2)
                    )
                } else {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Cy.Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            me?.name?.take(1)?.uppercase() ?: "C",
                            color = Cy.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        me?.name ?: tr("Akun Cyronime", "Cyronime account"),
                        color = Cy.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        me?.email ?: "",
                        color = Cy.Text2, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Daftar tautan ala web (divide-y, surface, radius-card)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Cy.RadiusCard))
                .background(Cy.Surface)
        ) {
            ProfileLink(Icons.Filled.Schedule, tr("Riwayat Tontonan", "Watch History"), tr("Episode yang sudah kamu tonton", "Episodes you have watched")) {
                nav.navigate("library/history")
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Cy.Line))
            ProfileLink(Icons.Filled.PlaylistPlay, tr("Favorit", "Favorites"), tr("Anime & donghua yang kamu simpan", "Anime & donghua you saved")) {
                nav.navigate("library/favorites")
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Cy.Line))
            ProfileLink(Icons.Filled.Settings, tr("Pengaturan", "Settings"), tr("Tema dan preferensi lainnya", "Theme and other preferences")) {
                nav.navigate("settings")
            }
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { logout() },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(tr("Logout", "Log out"), color = Cy.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileLink(icon: ImageVector, label: String, desc: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, label, tint = Cy.Text2, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Cy.Text2, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = Cy.Text2, modifier = Modifier.size(22.dp)
        )
    }
}


/* ============================================================
 * Settings — halaman pengaturan pengguna.
 *
 * Grup: Personalisasi, Pemutaran, Bahasa, Data & Privasi, Akun,
 * Tentang. Pilihan disimpan via AppSettings (SharedPreferences)
 * dan langsung memperbarui UI (state Compose).
 *
 * Sistem teknis (registrasi device, sinkron notifikasi, preferensi
 * notifikasi di server) TETAP berjalan — pemulihannya otomatis dan
 * senyap lewat SettingsActions, tidak lagi ditampilkan ke pengguna.
 * ============================================================ */
@Composable
fun SettingsScreen(nav: NavController, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    var me by remember { mutableStateOf<Me?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmHistory by remember { mutableStateOf(false) }
    var confirmProgress by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val io = rememberIoScope()

    // Akun + pemulihan senyap registrasi device (jalur utama tetap
    // onNewToken + MainActivity saat login; ini hanya pemantik ulang).
    LaunchedEffect(Unit) {
        try { me = Api.me() } catch (_: Exception) { me = null }
        SettingsActions.ensureDeviceRegistered(context)
    }

    fun runBusy(block: suspend () -> Unit, okMsg: String) {
        io.launch {
            busy = true
            try { block(); toast = okMsg } catch (_: Exception) { toast = tr("Gagal. Coba lagi.", "Failed. Try again.") }
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(tr("Pengaturan", "Settings"), color = Cy.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        /* ---------- Personalisasi ---------- */
        SettingsCard {
            CardHeader(Icons.Filled.Palette, tr("Personalisasi", "Personalization"))
            ChoiceRow(tr("Tema", "Theme"), AppSettings.ThemeMode.entries,
                labelOf = { m -> when (m) {
                    AppSettings.ThemeMode.System -> tr("Sistem", "System")
                    AppSettings.ThemeMode.Dark -> tr("Gelap", "Dark")
                    AppSettings.ThemeMode.Light -> tr("Terang", "Light") } },
                selected = { m -> AppSettings.themeMode == m },
                onPick = { AppSettings.setThemeMode(context, it) })
            CardDivider()
            ChoiceRow(tr("Warna aksen", "Accent color"), AppSettings.Accent.entries,
                labelOf = { a -> when (a) {
                    AppSettings.Accent.Purple -> tr("Ungu", "Purple")
                    AppSettings.Accent.Blue -> tr("Biru", "Blue")
                    AppSettings.Accent.Pink -> tr("Merah muda", "Pink") } },
                selected = { a -> AppSettings.accent == a },
                onPick = { AppSettings.setAccent(context, it) })
            CardDivider()
            ChoiceRow(tr("Ukuran teks", "Text size"), AppSettings.TextScale.entries,
                labelOf = { t -> when (t) {
                    AppSettings.TextScale.Small -> tr("Kecil", "Small")
                    AppSettings.TextScale.Normal -> tr("Normal", "Normal")
                    AppSettings.TextScale.Large -> tr("Besar", "Large") } },
                selected = { t -> AppSettings.textScale == t },
                onPick = { AppSettings.setTextScale(context, it) })
            CardDivider()
            ChoiceRow(tr("Kolom daftar", "Grid columns"), listOf(2, 3),
                labelOf = { c -> tr("${c} kolom", "$c columns") },
                selected = { c -> AppSettings.gridColumns == c },
                onPick = { AppSettings.setGridColumns(context, it) })
            CardDivider()
            ChoiceRow(tr("Rasio poster", "Poster ratio"), AppSettings.PosterRatio.entries,
                labelOf = { r -> if (r == AppSettings.PosterRatio.Portrait) "3:4" else "2:3" },
                selected = { r -> AppSettings.posterRatio == r },
                onPick = { AppSettings.setPosterRatio(context, it) })
        }

        Spacer(Modifier.height(20.dp))

        /* ---------- Pemutaran ---------- */
        SettingsCard {
            CardHeader(Icons.Filled.PlayCircle, tr("Pemutaran", "Playback"))
            ChoiceRow(tr("Kualitas video", "Video quality"), AppSettings.Quality.entries,
                labelOf = { q -> when (q) {
                    AppSettings.Quality.Auto -> tr("Otomatis", "Auto")
                    AppSettings.Quality.Q360 -> "360p"
                    AppSettings.Quality.Q480 -> "480p"
                    AppSettings.Quality.Q720 -> "720p" } },
                selected = { q -> AppSettings.quality == q },
                onPick = { AppSettings.setQuality(context, it) })
            CardDivider()
            SwitchRow(tr("Episode berikutnya", "Play next episode"),
                tr("Lanjut otomatis setelah selesai", "Continue automatically when finished"),
                AppSettings.autoPlayNext) { AppSettings.setAutoPlayNext(context, it) }
            CardDivider()
            SwitchRow(tr("Putar otomatis", "Autoplay"),
                tr("Mulai video begitu dibuka", "Start video as soon as it opens"),
                AppSettings.autoPlay) { AppSettings.setAutoPlay(context, it) }
            CardDivider()
            SwitchRow(tr("Ingat posisi terakhir", "Remember position"),
                tr("Lanjut dari posisi terakhir ditonton", "Resume where you left off"),
                AppSettings.rememberPosition) { AppSettings.setRememberPosition(context, it) }
        }

        Spacer(Modifier.height(20.dp))

        /* ---------- Bahasa ---------- */
        SettingsCard {
            CardHeader(Icons.Filled.Language, tr("Bahasa", "Language"))
            ChoiceRow("", AppSettings.Language.entries,
                labelOf = { l -> if (l == AppSettings.Language.Id) "Bahasa Indonesia" else "English" },
                selected = { l -> AppSettings.language == l },
                onPick = { AppSettings.setLanguage(context, it) })
        }

        Spacer(Modifier.height(20.dp))

        /* ---------- Data & Privasi ---------- */
        SettingsCard {
            CardHeader(Icons.Filled.DeleteSweep, tr("Data & Privasi", "Data & Privacy"))
            ActionRow(tr("Hapus riwayat tontonan", "Clear watch history"),
                tr("Menghapus riwayat episode di semua perangkat", "Removes episode history on all devices"),
                enabled = !busy) { confirmHistory = true }
            CardDivider()
            ActionRow(tr("Reset progress menonton", "Reset watch progress"),
                tr("Continue watching akan direset di semua perangkat", "Continue watching will reset on all devices"),
                enabled = !busy) { confirmProgress = true }
        }

        Spacer(Modifier.height(20.dp))

        /* ---------- Akun ---------- */
        SettingsCard {
            CardHeader(Icons.Filled.Person, tr("Akun", "Account"))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!me?.image.isNullOrBlank()) {
                    AsyncImage(
                        model = me!!.image,
                        contentDescription = tr("Foto profil", "Profile photo"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                    )
                } else {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Cy.Accent), contentAlignment = Alignment.Center) {
                        Text(me?.name?.take(1)?.uppercase() ?: "C",
                            color = Cy.OnMedia, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(me?.name ?: tr("Akun Cyronime", "Cyronime account"),
                        color = Cy.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    me?.email?.let {
                        Text(it, color = Cy.Text2, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            CardDivider()
            ActionRow(tr("Keluar dari akun", "Log out"),
                tr("Data Anda tetap tersimpan di akun", "Your data stays saved in your account"),
                enabled = !busy, danger = true) { confirmLogout = true }
        }

        Spacer(Modifier.height(20.dp))

        /* ---------- Tentang ---------- */
        SettingsCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(Cy.RadiusMd)).background(Cy.Accent),
                    contentAlignment = Alignment.Center
                ) { Text("C", color = Cy.OnMedia, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f)) {
                    Text("Cyronime", color = Cy.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(tr("Versi ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"),
                        color = Cy.Text2, fontSize = 13.sp)
                }
            }
            CardDivider()
            Text(
                tr(
                    "Nonton anime & donghua subtitle Indonesia dengan pengalaman tanpa gangguan.",
                    "Watch anime & donghua with Indonesian subtitles, distraction-free."
                ),
                color = Cy.Text2, fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            CardDivider()
            ActionRow(tr("Kebijakan Privasi", "Privacy Policy"), null, enabled = !busy) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cyronime.web.id/privacy")))
            }
        }

        toast?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = Cy.Text2, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = tr("Keluar dari Cyronime?", "Log out of Cyronime?"),
            text = tr("Riwayat dan favorit Anda tetap tersimpan di akun.", "Your history and favorites stay saved in your account."),
            confirmLabel = tr("Logout", "Log out"),
            onConfirm = {
                confirmLogout = false
                io.launch {
                    busy = true
                    try {
                        SettingsActions.logout(context)
                        withContext(Dispatchers.Main) { onLoggedOut() }
                    } catch (_: Exception) {
                        toast = tr("Gagal. Coba lagi.", "Failed. Try again.")
                    }
                    busy = false
                }
            },
            onDismiss = { confirmLogout = false }
        )
    }
    if (confirmHistory) {
        ConfirmDialog(
            title = tr("Hapus semua riwayat tontonan?", "Clear all watch history?"),
            text = tr(
                "Riwayat episode di akun Anda akan dihapus permanen di semua perangkat. Tindakan ini tidak dapat dibatalkan.",
                "Episode history in your account will be permanently removed on all devices. This cannot be undone."
            ),
            confirmLabel = tr("Hapus", "Clear"),
            onConfirm = {
                confirmHistory = false
                runBusy({ SettingsActions.clearWatchData(SettingsActions.Kind.History) },
                    tr("Riwayat tontonan dihapus.", "Watch history cleared."))
            },
            onDismiss = { confirmHistory = false }
        )
    }
    if (confirmProgress) {
        ConfirmDialog(
            title = tr("Reset progress menonton?", "Reset watch progress?"),
            text = tr(
                "Posisi tontonan dan Continue watching akan dihapus dari semua perangkat. Tindakan ini tidak dapat dibatalkan.",
                "Watch positions and Continue watching will be removed from all devices. This cannot be undone."
            ),
            confirmLabel = tr("Reset", "Reset"),
            onConfirm = {
                confirmProgress = false
                runBusy({ SettingsActions.clearWatchData(SettingsActions.Kind.Progress) },
                    tr("Progress tontonan direset.", "Watch progress reset."))
            },
            onDismiss = { confirmProgress = false }
        )
    }
}

/* ---------- Komponen kartu ---------- */

/** Kartu pengaturan: surface radius-card, konten bertingkat. */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Cy.RadiusCard))
            .background(Cy.Surface),
        content = content
    )
}

@Composable
private fun CardHeader(icon: ImageVector, title: String) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = Cy.Accent, modifier = Modifier.size(18.dp))
        Text(title, color = Cy.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CardDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(Cy.Line))
}

/** Baris pilihan multi-nilai (tema, aksen, dsb.): segmen pil, klik untuk memilih. */
@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    labelOf: @Composable (T) -> String,
    selected: (T) -> Boolean,
    onPick: (T) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label.isNotBlank()) {
            Text(label, color = Cy.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Cy.Surface2)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { opt ->
                val on = selected(opt)
                Text(
                    labelOf(opt),
                    color = if (on) Cy.OnMedia else Cy.Text2,
                    fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) Cy.Accent else Color.Transparent)
                        .clickable { onPick(opt) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** Baris toggle. */
@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Cy.Text, fontSize = 15.sp)
            Text(subtitle, color = Cy.Text2, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Cy.Accent,
                checkedThumbColor = Cy.OnMedia,
                uncheckedTrackColor = Cy.Surface2,
                uncheckedThumbColor = Cy.Text2
            )
        )
    }
}

/** Baris aksi (klik). */
@Composable
private fun ActionRow(title: String, subtitle: String?, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) Cy.Peach else Cy.Text, fontSize = 15.sp, fontWeight = if (danger) FontWeight.SemiBold else FontWeight.Normal)
            subtitle?.let { Text(it, color = Cy.Text2, fontSize = 12.sp) }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = Cy.Text2, modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Cy.Surface,
        title = { Text(title, color = Cy.Text) },
        text = { Text(text, color = Cy.Text2, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (confirmLabel == tr("Logout", "Log out")) Cy.Accent else Cy.Peach)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel"), color = Cy.Text2) }
        }
    )
}
