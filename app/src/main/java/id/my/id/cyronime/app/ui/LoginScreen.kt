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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
                    "Login Google gagal [" + e.javaClass.simpleName + "] " + (e.message ?: "")
                busy = false
            } catch (e: Exception) {
                error = "Login gagal: " + ((e.message ?: "").ifBlank { e.javaClass.simpleName }) + " [" + e.javaClass.simpleName + "]"
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Cy.Navy).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id.my.id.cyronime.app.R.drawable.airin),
            contentDescription = "Airin",
            modifier = Modifier.size(180.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text("Masuk ke Cyronime", color = Cy.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Akun yang sama dengan Cyronime Web. History, favorit, dan progress tontonan langsung tersinkron.",
            color = Cy.Text2, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(Modifier.height(32.dp))

        if (!loggedIn) {
            Row(
                Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(Cy.RadiusMd))
                    .background(if (busy) Cy.Surface2 else Cy.Accent)
                    .clickable(enabled = !busy) { signIn() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Cy.Text)
                    Spacer(Modifier.size(10.dp))
                }
                Text(if (busy) "Memproses..." else "Lanjutkan dengan Google",
                    color = Cy.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Sudah login.", color = Cy.Text2, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.height(52.dp).clip(RoundedCornerShape(Cy.RadiusMd)).background(Cy.Accent)
                    .clickable { onDone() }.padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) { Text("Lanjut menonton", color = Cy.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }

        if (error != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                error ?: "",
                color = Cy.Peach,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
