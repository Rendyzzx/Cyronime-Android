package id.my.id.cyronime.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.messaging.FirebaseMessaging
import id.my.id.cyronime.app.BuildConfig
import id.my.id.cyronime.app.Prefs
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.Me
import id.my.id.cyronime.app.data.NotifyPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings: profil akun (session server), preferensi notifikasi (backend,
 * sinkron dengan Web), info device, update & logout.
 */
@Composable
fun SettingsScreen(nav: NavController, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    var me by remember { mutableStateOf<Me?>(null) }
    var prefs by remember { mutableStateOf(NotifyPrefs.DEFAULT) }
    var loading by remember { mutableStateOf(true) }
    var confirmLogout by remember { mutableStateOf(false) }

    fun reload() {
        loading = true
        CoroutineScope(Dispatchers.IO).launch {
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Api.setNotifyPref(key, next)
            } catch (_: Exception) {
            }
        }
    }

    fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
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
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        /* --- Akun --- */
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (me?.image != null) {
                AsyncImage(
                    model = me!!.image,
                    contentDescription = "Foto profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(me?.name ?: "Akun Cyronime", style = MaterialTheme.typography.titleMedium)
                Text(
                    me?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Notifications", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tersimpan di akun Anda — berlaku juga di Web.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        PrefToggle("Episode baru", prefs.newEpisode, loading) { prefs = prefs.copy(newEpisode = it); togglePref("newEpisode", it) }
        PrefToggle("Anime favorit", prefs.favorite, loading) { prefs = prefs.copy(favorite = it); togglePref("favorite", it) }
        PrefToggle("Pengumuman Cyronime", prefs.announcement, loading) { prefs = prefs.copy(announcement = it); togglePref("announcement", it) }
        PrefToggle("Maintenance", prefs.maintenance, loading) { prefs = prefs.copy(maintenance = it); togglePref("maintenance", it) }
        PrefToggle("Update aplikasi", prefs.appUpdate, loading) { prefs = prefs.copy(appUpdate = it); togglePref("appUpdate", it) }

        Spacer(Modifier.height(24.dp))
        Text("Perangkat", style = MaterialTheme.typography.titleMedium)
        Text(
            "ID: ${Prefs.deviceId(context).take(18)}…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = {
            // Ambil token FCM segar & register ulang (mis. setelah ganti ROM/reinstall).
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Prefs.setFcmToken(context, token)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Api.registerDevice(context, token)
                    } catch (_: Exception) {
                    }
                }
            }
        }) { Text("Sinkronkan ulang notifikasi") }

        Spacer(Modifier.height(24.dp))
        Text("Tentang", style = MaterialTheme.typography.titleMedium)
        Text(
            "Cyronime untuk Android ${BuildConfig.VERSION_NAME} • backend: ${Api.base.removePrefix("https://")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Button(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Logout")
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Logout dari Cyronime?") },
            text = { Text("History dan favorit Anda tetap tersimpan di akun.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    logout()
                }) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Batal") }
            }
        )
    }
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
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
