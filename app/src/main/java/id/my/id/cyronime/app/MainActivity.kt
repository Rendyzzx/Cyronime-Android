package id.my.id.cyronime.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import id.my.id.cyronime.app.Prefs
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.AppVersion
import id.my.id.cyronime.app.data.SystemStatus
import id.my.id.cyronime.app.ui.CyronimeTheme
import id.my.id.cyronime.app.ui.DetailScreen
import id.my.id.cyronime.app.ui.HomeScreen
import id.my.id.cyronime.app.ui.LibraryScreen
import id.my.id.cyronime.app.ui.LoginScreen
import id.my.id.cyronime.app.ui.OnboardingScreen
import id.my.id.cyronime.app.ui.MaintenanceScreen
import id.my.id.cyronime.app.ui.SearchScreen
import id.my.id.cyronime.app.ui.SettingsScreen
import id.my.id.cyronime.app.ui.WatchScreen
import id.my.id.cyronime.app.ui.versionLessThan
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val deepLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        askNotificationPermission()

        setContent {
            CyronimeTheme {
                CyronimeApp(deepLink.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    /** cyronime://anime/{slug} | cyronime://anime/watch/{episodeId} | https://cyronime.web.id/... */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val host = data.host ?: return
        val segments = (data.pathSegments ?: emptyList<String>()).toMutableList()
        if (host != "anime" && host != "donghua") return
        if (segments.size >= 2 && segments[0] == "watch") {
            deepLink.value = "$host/watch/${segments[1]}"
        } else if (segments.size >= 1) {
            deepLink.value = "$host/${segments[0]}"
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }
}

/**
 * Root UI: cek maintenance & versi saat startup / kembali foreground
 * (bukan polling), lalu tampilkan navigasi utama.
 */
@Composable
fun CyronimeApp(initialDeepLink: String?) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var maintenance by remember { mutableStateOf<SystemStatus?>(null) }
    var forceVersion by remember { mutableStateOf<AppVersion?>(null) }
    var softVersion by remember { mutableStateOf<AppVersion?>(null) }
    var sessionActive by remember { mutableStateOf(Api.hasSession()) }

    suspend fun checkSystem() {
        try {
            val status = Api.systemStatus()
            maintenance = if (status.maintenance) status else null
        } catch (_: Exception) {
            // Offline saat cek: jangan blokir app dengan asumsi salah.
            maintenance = null
        }
    }

    suspend fun checkVersion() {
        try {
            val v = Api.appVersion()
            if (v.forceUpdate || versionLessThan(BuildConfig.VERSION_NAME, v.minimumVersion)) {
                forceVersion = v
            } else if (versionLessThan(BuildConfig.VERSION_NAME, v.latestVersion)) {
                softVersion = v
            }
        } catch (_: Exception) {
            // Update check gagal tidak boleh menghalangi pemakaian.
        }
    }

    LaunchedEffect(Unit) {
        checkSystem()
        checkVersion()
    }

    // Cek maintenance saat app kembali ke foreground — tanpa polling.
    LifecycleResumeEffect(Unit) {
        scope.launch { checkSystem() }
        onPauseOrDispose { }
    }

    // Deep link dari notification / browser (hanya jika sudah login).
    LaunchedEffect(initialDeepLink) {
        val link = initialDeepLink ?: return@LaunchedEffect
        if (!Api.hasSession()) return@LaunchedEffect
        val parts = link.split("/")
        when {
            parts.size >= 3 && parts[1] == "watch" ->
                navController.navigate("watch/${parts[0]}/${parts[2]}")
            parts.size >= 2 ->
                navController.navigate("detail/${parts[0]}/${parts[1]}")
        }
    }

    /* --- Maintenance (server-side): layar penuh, tidak bisa dilewati --- */
    maintenance?.let { status ->
        MaintenanceScreen(status, retry = {
            maintenance = null
            scope.launch { checkSystem() }
        })
        return
    }

    /* --- Force update: versi di bawah minimum --- */
    forceVersion?.let { v ->
        AlertDialog(
            onDismissRequest = { /* tidak bisa ditutup */ },
            title = { Text("Versi aplikasi Anda sudah tidak didukung.") },
            text = {
                Text(
                    "Minimum versi yang didukung adalah ${v.minimumVersion}. " +
                        "Perbarui aplikasi untuk melanjutkan."
                )
            },
            confirmButton = {
                Button(onClick = {
                    val url = v.downloadUrl
                    if (url != null) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }) { Text("Update Sekarang") }
            }
        )
        return
    }

    /* --- Update tersedia (opsional, tidak dipaksa) --- */
    softVersion?.let { v ->
        AlertDialog(
            onDismissRequest = { softVersion = null },
            title = { Text("Update tersedia.") },
            text = { Text("Versi terbaru Cyronime: ${v.latestVersion}.") },
            confirmButton = {
                TextButton(onClick = {
                    softVersion = null
                    val url = v.downloadUrl
                    if (url != null) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { softVersion = null }) { Text("Nanti") }
            }
        )
    }

    val start = if (sessionActive && Prefs.onboardingDone(context)) "home"
        else if (sessionActive) "onboarding" else "onboarding"
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomNav = currentRoute == "home" || currentRoute == "search" ||
        currentRoute == "library" || currentRoute == "settings"

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                CyBottomBar(
                    current = currentRoute,
                    isAnime = Prefs.portal(context) == "anime",
                    onTab = { navController.goTab(it) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding)
        ) {
            composable("onboarding") {
                // Sama seperti Web: Splash > Disclaimer > Airin > Carousel > LOGIN > Pilih Tontonan.
                OnboardingScreen(
                    loggedIn = sessionActive,
                    loginContent = { done ->
                        LoginScreen(onDone = { sessionActive = true; done() })
                    },
                    onPick = { portal ->
                        Prefs.setPortal(context, portal)
                        Prefs.setOnboardingDone(context, true)
                        navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                    }
                )
            }
            composable("login") {
                LoginScreen(onDone = {
                    sessionActive = true
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                })
            }
            composable("home") { HomeScreen(navController) }
            composable("search") { SearchScreen(navController) }
            composable("library") { LibraryScreen(navController) }
            composable("settings") {
                SettingsScreen(navController, onLoggedOut = {
                    sessionActive = false
                    navController.navigate("onboarding") { popUpTo(0) }
                })
            }
            composable("detail/{type}/{slug}") { entry ->
                val type = entry.arguments?.getString("type") ?: "anime"
                val slug = entry.arguments?.getString("slug") ?: return@composable
                DetailScreen(navController, type, slug)
            }
            composable("watch/{type}/{id}") { entry ->
                val type = entry.arguments?.getString("type") ?: "anime"
                val id = entry.arguments?.getString("id") ?: return@composable
                WatchScreen(navController, type, id)
            }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}


/** Bottom nav meniru Web: 68dp, latar --surface, garis atas tipis, label hanya di item aktif. */
@Composable
private fun CyBottomBar(current: String?, isAnime: Boolean, onTab: (String) -> Unit) {
    val items = listOf(
        Triple("home", "Home", androidx.compose.material.icons.Icons.Filled.Home),
        Triple("search", "Cari", androidx.compose.material.icons.Icons.Filled.Search),
        Triple("library", "Tontonanku", androidx.compose.material.icons.Icons.Filled.PlayArrow),
        Triple("settings", "Profil", androidx.compose.material.icons.Icons.Filled.Person)
    )
    androidx.compose.foundation.layout.Column(
        androidx.compose.ui.Modifier.background(id.my.id.cyronime.app.ui.Cy.Surface)
    ) {
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(id.my.id.cyronime.app.ui.Cy.Line)
        )
        androidx.compose.foundation.layout.Row(
            androidx.compose.ui.Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            items.forEach { (route, label, icon) ->
                val active = current == route
                androidx.compose.foundation.layout.Column(
                    androidx.compose.ui.Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onTab(route) },
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    androidx.compose.material3.Icon(
                        icon, contentDescription = label,
                        tint = if (active) id.my.id.cyronime.app.ui.Cy.Text else id.my.id.cyronime.app.ui.Cy.Text2,
                        modifier = androidx.compose.ui.Modifier.size(24.dp)
                    )
                    if (active) {
                        Text(
                            label,
                            color = id.my.id.cyronime.app.ui.Cy.Text,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
