package id.my.id.cyronime.app.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.my.id.cyronime.app.Prefs
import id.my.id.cyronime.app.data.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Login via web Cyronime di dalam WebView (Google OAuth) — cookie session
 * Auth.js (httpOnly) mendar di CookieManager platform, lalu dipakai OkHttp
 * untuk semua request API (lihat WebViewCookieJar di data/Api.kt).
 *
 * Tidak ada registrasi manual: sistem existing hanya Google OAuth
 * (provider tester hanya untuk QA di development, nonaktif di produksi).
 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var sessionReady by remember { mutableStateOf(Api.hasSession()) }
    var loading by remember { mutableStateOf(true) }

    // Poll cookie session (loop ringan 1 detik, hanya di layar login).
    LaunchedEffect(Unit) {
        while (!sessionReady) {
            if (Api.hasSession()) {
                sessionReady = true
                break
            }
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Login Cyronime", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Masuk dengan akun Google — sama dengan akun Web Anda. " +
                    "History, favorit, dan progress langsung tersinkron.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            loading = true
                            if (Api.hasSession()) sessionReady = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            if (Api.hasSession()) sessionReady = true
                        }
                    }
                    loadUrl(Api.base + "/login")
                }
            }
        )
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Column(Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    // Registrasi token FCM (bila sudah tersedia) setelah login.
                    val token = Prefs.fcmToken(context)
                    if (token != null) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                Api.registerDevice(context, token)
                            } catch (_: Exception) {
                            }
                        }
                    }
                    Prefs.setSessionDone(context, true)
                    onDone()
                },
                enabled = sessionReady,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (sessionReady) "Selesai — Masuk ke Cyronime" else "Menunggu login Google…")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (sessionReady)
                    "Login terdeteksi. Jika alur onboarding di layar atas belum selesai, " +
                        "selesaikan dulu (pilih Anime/Donghua) lalu tekan tombol ini."
                else
                    "Selesaikan login Google di layar atas. Tombol aktif otomatis setelah berhasil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
