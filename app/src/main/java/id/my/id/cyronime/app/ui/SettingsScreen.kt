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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import id.my.id.cyronime.app.data.Me
import id.my.id.cyronime.app.data.NotifyPrefs
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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
            try {
                Api.unregisterDevice(context)
            } catch (_: Exception) {
            }
            try {
                Api.logout()
            } catch (_: Exception) {
            }
            Api.clearSession()
            Prefs.setSessionDone(context, false)
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
        Text("Profile", color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                        me?.name ?: "Akun Cyronime",
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
            ProfileLink(Icons.Filled.Schedule, "Watch History", "Riwayat episode yang sudah ditonton") {
                nav.navigate("library/history")
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Cy.Line))
            ProfileLink(Icons.Filled.PlaylistPlay, "Favorites", "Anime & donghua yang kamu simpan") {
                nav.navigate("library/favorites")
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Cy.Line))
            ProfileLink(Icons.Filled.Settings, "Settings", "Tema dan preferensi lainnya") {
                nav.navigate("settings")
            }
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { logout() },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Logout", color = Cy.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

/**
 * Settings ala SettingsView web: kartu akun, preferensi notifikasi (backend,
 * sinkron dengan Web), perangkat, tentang, aksi berbahaya (hapus history /
 * reset progress) dan logout.
 */
@Composable
fun SettingsScreen(nav: NavController, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    var me by remember { mutableStateOf<Me?>(null) }
    var prefs by remember { mutableStateOf(NotifyPrefs.DEFAULT) }
    var loading by remember { mutableStateOf(true) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmHistory by remember { mutableStateOf(false) }
    var confirmProgress by remember { mutableStateOf(false) }

    val io = rememberIoScope()

    fun reload() {
        loading = true
        io.launch {
            try {
                me = Api.me()
                prefs = Api.getNotifyPrefs()
            } catch (_: Exception) {
                me = null
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    fun togglePref(key: String, next: Boolean) {
        io.launch {
            try {
                Api.setNotifyPref(key, next)
            } catch (_: Exception) {
            }
        }
    }

    fun logout() {
        io.launch {
            try {
                Api.unregisterDevice(context)
            } catch (_: Exception) {
            }
            try {
                Api.logout()
            } catch (_: Exception) {
            }
            Api.clearSession()
            Prefs.setSessionDone(context, false)
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
        Text("Settings", color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        /* --- Kartu akun --- */
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Cy.RadiusCard))
                .background(Cy.Surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!me?.image.isNullOrBlank()) {
                AsyncImage(
                    model = me!!.image,
                    contentDescription = "Foto profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            }
            Column {
                Text(
                    me?.name ?: "Akun Cyronime",
                    color = Cy.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
                Text(me?.email ?: "", color = Cy.Text2, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        /* --- Notifications --- */
        SettingsSectionLabel("Notifications", "Tersimpan di akun Anda — berlaku juga di Web.")
        Spacer(Modifier.height(8.dp))
        PrefToggle("Episode baru", prefs.newEpisode, loading) { prefs = prefs.copy(newEpisode = it); togglePref("newEpisode", it) }
        PrefToggle("Anime favorit", prefs.favorite, loading) { prefs = prefs.copy(favorite = it); togglePref("favorite", it) }
        PrefToggle("Pengumuman Cyronime", prefs.announcement, loading) { prefs = prefs.copy(announcement = it); togglePref("announcement", it) }
        PrefToggle("Maintenance", prefs.maintenance, loading) { prefs = prefs.copy(maintenance = it); togglePref("maintenance", it) }
        PrefToggle("Update aplikasi", prefs.appUpdate, loading) { prefs = prefs.copy(appUpdate = it); togglePref("appUpdate", it) }

        Spacer(Modifier.height(24.dp))

        /* --- Perangkat --- */
        SettingsSectionLabel("Perangkat", "ID: ${Prefs.deviceId(context).take(18)}…")
        TextButton(onClick = {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Prefs.setFcmToken(context, token)
                io.launch {
                    try {
                        Api.registerDevice(context, token)
                    } catch (_: Exception) {
                    }
                }
            }
        }) { Text("Sinkronkan ulang notifikasi") }

        Spacer(Modifier.height(24.dp))

        /* --- Tentang --- */
        SettingsSectionLabel(
            "Tentang",
            "Cyronime untuk Android ${BuildConfig.VERSION_NAME} • backend: ${Api.base.removePrefix("https://")}"
        )

        Spacer(Modifier.height(24.dp))

        /* --- Aksi berbahaya (endpoint sama dengan web) --- */
        SettingsSectionLabel("Data tontonan", "Hapus history atau reset progress di semua perangkat.")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(Cy.RadiusMd))
                    .background(Cy.Surface)
                    .clickable { confirmHistory = true },
                contentAlignment = Alignment.Center
            ) {
                Text("Hapus History", color = Cy.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(Cy.RadiusMd))
                    .background(Cy.Surface)
                    .clickable { confirmProgress = true },
                contentAlignment = Alignment.Center
            ) {
                Text("Reset Progress", color = Cy.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Logout")
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Logout dari Cyronime?",
            text = "History dan favorit Anda tetap tersimpan di akun.",
            confirmLabel = "Logout",
            onConfirm = { confirmLogout = false; logout() },
            onDismiss = { confirmLogout = false }
        )
    }
    if (confirmHistory) {
        ConfirmDialog(
            title = "Hapus semua history?",
            text = "Riwayat tontonan di akun Anda akan dihapus permanen.",
            confirmLabel = "Hapus",
            onConfirm = {
                confirmHistory = false
                io.launch {
                    try {
                        deleteAll("history")
                    } catch (_: Exception) {
                    }
                }
            },
            onDismiss = { confirmHistory = false }
        )
    }
    if (confirmProgress) {
        ConfirmDialog(
            title = "Reset progress tontonan?",
            text = "Continue watching di semua perangkat akan direset.",
            confirmLabel = "Reset",
            onConfirm = {
                confirmProgress = false
                io.launch {
                    try {
                        deleteAll("progress")
                    } catch (_: Exception) {
                    }
                }
            },
            onDismiss = { confirmProgress = false }
        )
    }
}

/** DELETE history (semua) / progress (?all=1) — endpoint sama dengan web. */
private suspend fun deleteAll(kind: String) {
    kotlinx.coroutines.withContext(Dispatchers.IO) {
        val path = if (kind == "history") "/api/history" else "/api/watch/progress?all=1"
        val req = Request.Builder()
            .url(Api.base + path)
            .header("User-Agent", "Cyronime-Android/" + BuildConfig.VERSION_NAME)
            .delete()
            .build()
        Api.client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String, subtitle: String) {
    Text(title, color = Cy.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    Text(subtitle, color = Cy.Text2, fontSize = 12.sp)
}

@Composable
private fun PrefToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Cy.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Cy.Accent,
                checkedThumbColor = Cy.Text,
                uncheckedTrackColor = Cy.Surface2,
                uncheckedThumbColor = Cy.Text2
            )
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
                Text(confirmLabel, color = if (confirmLabel == "Logout") Cy.Accent else Cy.Peach)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Cy.Text2) }
        }
    )
}
