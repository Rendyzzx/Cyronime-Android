package id.my.id.cyronime.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import id.my.id.cyronime.app.R
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.Me
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Login NATIVE (tanpa WebView). Dua jalur, dua-duanya menuju backend yang sama
 * (/api/auth/callback/google-idtoken, provider "google-idtoken"):
 *
 * 1. UTAMA: Credential Manager (GetGoogleIdOption) — sheet akun bawaan Android.
 * 2. FALLBACK OTOMATIS: Google Sign-In lama (play-services-auth).
 *
 * Kenapa fallback: di beberapa perangkat (terutama Xiaomi/MIUI dan beberapa
 * versi Play Services) ada bug sistem: user SUDAH memilih akun di sheet,
 * tetapi API mengembalikan GetCredentialCancellationException seolah user
 * menutup sheet — login diam-diam gagal tanpa error (bug Okt 2026 di HP
 * owner). Saat "cancellation" diterima, app otomatis beralih ke jalur 2.
 * Identitas dua-duanya sama (Google sub) — session pun sama dengan Web.
 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var loggedIn by remember { mutableStateOf(Api.hasSession()) }

    /** Kirim ID token ke backend, buat session, lalu selesai. */
    fun completeWithToken(idToken: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val me: Me = Api.nativeLogin(idToken)
                loggedIn = true
                busy = false
                onDone()
            } catch (e: Exception) {
                error = "Login gagal: " + ((e.message ?: "").ifBlank { e.javaClass.simpleName }) +
                    " [" + e.javaClass.simpleName + "]"
                busy = false
            }
        }
    }

    val compatLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        busy = false
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                error = "Google tidak mengirim token (mode kompatibel)."
            } else {
                busy = true
                completeWithToken(idToken)
            }
        } catch (e: ApiException) {
            // 12501 = user benar-benar menutup pilihan akun -> bukan error.
            if (e.statusCode != 12501) {
                error = "Mode kompatibel gagal [" + e.statusCode + "] " + (e.message ?: "")
            }
        } catch (e: Exception) {
            error = "Mode kompatibel gagal: " + (e.message ?: e.javaClass.simpleName)
        }
    }

    /** Jalur 2: Google Sign-In lama — stabil di perangkat yang menutup sheet. */
    fun signInCompat() {
        busy = true
        error = null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(id.my.id.cyronime.app.BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        try {
            compatLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
        } catch (e: Exception) {
            busy = false
            error = "Mode kompatibel tidak tersedia: " + (e.message ?: e.javaClass.simpleName)
        }
    }

    /** Jalur 1: Credential Manager sheet. */
    fun signIn() {
        if (busy) return
        busy = true
        error = null
        status = null

        val credentialManager = CredentialManager.create(context)
        val googleOption = GetGoogleIdOption.Builder()
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
                completeWithToken(idToken)
            } catch (e: GetCredentialCancellationException) {
                // Bug perangkat: pilih akun sukses tapi sistem laporkan "cancel"
                // (umum di Xiaomi/MIUI & sebagian versi Play Services). Coba jalur 2.
                status = "Perangkat menutup pilihan akun. Mencoba mode kompatibel..."
                signInCompat()
            } catch (e: GetCredentialException) {
                // Beberapa perangkat juga melaporkan kegagalan sheet sebagai error
                // umum — coba jalur 2 sekali sebelum menyerah.
                status = "Sheet akun gagal (" + e.javaClass.simpleName + "). Mencoba mode kompatibel..."
                signInCompat()
            } catch (e: Exception) {
                error = "Login gagal: " + ((e.message ?: "").ifBlank { e.javaClass.simpleName }) +
                    " [" + e.javaClass.simpleName + "]"
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Cy.Navy).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.airin),
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
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = { signInCompat() }) {
                Text("Pakai mode kompatibel", color = Cy.Text2, fontSize = 13.sp)
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

        if (status != null) {
            Spacer(Modifier.height(16.dp))
            Text(status ?: "", color = Cy.Text2, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
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
