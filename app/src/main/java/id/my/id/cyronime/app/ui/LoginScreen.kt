package id.my.id.cyronime.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.Me
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Login NATIVE (tanpa WebView): Google Sign-In via Android Credential
 * Manager (GetGoogleIdOption). App mendapatkan Google ID Token, lalu
 * dikirim ke backend (/api/auth/callback/credentials, provider
 * "google-idtoken") yang memverifikasi token di server dan menerbitkan
 * session Auth.js yang SAMA dengan login Web (cookie session-token
 * mendar di CookieManager, dipakai OkHttp & WebView embed).
 *
 * Identitas konsisten: backend memakai Google `sub` — history, favorit,
 * dan progress Web/Android akun yang sama.
 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loggedIn by remember { mutableStateOf(Api.hasSession()) }

    fun signIn() {
        if (busy) return
        busy = true
        error = null

        val credentialManager = CredentialManager.create(context)
        val googleOption = GetGoogleIdOption.Builder()
            // Web client (public, bukan rahasia) — audience ID token.
            .setServerClientId(id.my.id.cyronime.app.BuildConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val idToken = (result.credential as? CustomCredential)?.let { cred ->
                    if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(cred.data).idToken
                    } else null
                }
                if (idToken == null) {
                    error = "Login Google tidak tersedia di perangkat ini (butuh Google Play Services)."
                    busy = false
                    return@launch
                }
                // Session Auth.js diterbitkan backend; cookie tersimpan di CookieManager.
                val me: Me = Api.nativeLogin(idToken)
                loggedIn = true
                busy = false
                onDone()
            } catch (e: GetCredentialCancellationException) {
                // Pengguna menutup sheet — bukan error.
                busy = false
            } catch (e: GetCredentialException) {
                error =
                    "Login Google gagal (" + e.javaClass.simpleName + "). " +
                    "Pastikan SHA-1 aplikasi sudah didaftarkan di Google Cloud Console " +
                    "dan perangkat memakai Google Play Services terbaru."
                busy = false
            } catch (e: Exception) {
                val msg = e.message ?: ""
                error = if (msg.contains("401")) "Login ditolak server. Coba lagi."
                else "Gagal terhubung: " + (msg.ifBlank { e.javaClass.simpleName })
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cyronime", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Anime & Donghua",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (!loggedIn) {
            Button(
                onClick = { signIn() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(10.dp))
                }
                Text(if (busy) "Memproses…" else "Lanjutkan dengan Google")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Akun yang sama dengan Cyronime Web — history, favorit, dan " +
                    "progress langsung tersinkron.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text("Sudah login.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { onDone() }) { Text("Lanjut menonton") }
        }

        if (error != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
