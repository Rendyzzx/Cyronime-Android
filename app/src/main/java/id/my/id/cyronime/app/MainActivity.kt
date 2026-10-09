package id.my.id.cyronime.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import id.my.id.cyronime.app.data.Api
import id.my.id.cyronime.app.data.AppVersion
import id.my.id.cyronime.app.data.Me
import id.my.id.cyronime.app.data.SystemStatus
import id.my.id.cyronime.app.ui.Cy
import id.my.id.cyronime.app.ui.CyronimeTheme
import id.my.id.cyronime.app.ui.DetailScreen
import id.my.id.cyronime.app.ui.HomeScreen
import id.my.id.cyronime.app.ui.LibraryScreen
import id.my.id.cyronime.app.ui.LoginScreen
import id.my.id.cyronime.app.ui.OnboardingScreen
import id.my.id.cyronime.app.ui.PortalListScreen
import id.my.id.cyronime.app.ui.ProfileScreen
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
 * (bukan polling), lalu tampilkan navigasi utama. Struktur nav meniru web:
 * bottom nav = Home, portal (Anime/Donghua), Cari, Profil; history &
 * favorites dibuka dari Profil (seperti halaman /profile web).
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
    var portalAnime by remember { mutableStateOf(Prefs.portal(context) == "anime") }
    var me by remember { mutableStateOf<Me?>(null) }

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

    suspend fun loadMe() {
        if (!Api.hasSession()) return
        try {
            me = Api.me()
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(Unit) {
        checkSystem()
        checkVersion()
        loadMe()
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

    // Start destination dikunci SEKALI (remember) agar login di tengah onboarding
    // tidak membuat NavHost menghitung ulang & melompat ke Home. Home hanya
    // dibuka langsung jika sesi ada DAN portal sudah dipilih user.
    val start = remember {
        if (Api.hasSession() && Prefs.onboardingDone(context) && Prefs.portalChosen(context)) "home"
        else "onboarding"
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomNav = currentRoute == "home" || currentRoute == "search" ||
        currentRoute == "profile" || currentRoute == "portal/{type}"

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                CyBottomBar(
                    current = currentRoute,
                    portalAnime = portalAnime,
                    me = me,
                    onTab = { route ->
                        if (route == "portal") {
                            navController.goTab(if (portalAnime) "portal/anime" else "portal/donghua")
                        } else {
                            navController.goTab(route)
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
            enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(120)) },
            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(120)) },
            popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)) }
        ) {
            composable("onboarding") {
                // Sama seperti Web: Splash > Disclaimer > Airin > Carousel > LOGIN > Pilih Tontonan.
                OnboardingScreen(
                    loggedIn = sessionActive,
                    loginContent = { done ->
                        LoginScreen(onDone = {
                            sessionActive = true
                            scope.launch { loadMe() }
                            done()
                        })
                    },
                    onPick = { portal ->
                        Prefs.setPortal(context, portal)
                        Prefs.setPortalChosen(context, true)
                        Prefs.setOnboardingDone(context, true)
                        portalAnime = portal == "anime"
                        navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                    }
                )
            }
            composable("login") {
                LoginScreen(onDone = {
                    sessionActive = true
                    scope.launch { loadMe() }
                    // Selalu lewat Pilih Tontonan dulu, jangan langsung ke Home.
                    navController.navigate(
                        if (Prefs.portalChosen(context)) "home" else "onboarding"
                    ) { popUpTo("login") { inclusive = true } }
                })
            }
            composable("home") {
                HomeScreen(navController)
                // Portal bisa berubah dari dalam Home (PortalSwitch).
                LaunchedEffect(Unit) { portalAnime = Prefs.portal(context) == "anime" }
            }
            composable("portal/{type}") { entry ->
                val type = entry.arguments?.getString("type") ?: "anime"
                PortalListScreen(navController, type)
                LaunchedEffect(type) { portalAnime = type == "anime" }
            }
            composable("search") { SearchScreen(navController) }
            composable("profile") {
                ProfileScreen(navController, onLoggedOut = {
                    sessionActive = false
                    me = null
                    navController.navigate("onboarding") { popUpTo(0) }
                })
            }
            composable("settings") {
                SettingsScreen(navController, onLoggedOut = {
                    sessionActive = false
                    me = null
                    navController.navigate("onboarding") { popUpTo(0) }
                })
            }
            composable("library/{mode}") { entry ->
                val mode = entry.arguments?.getString("mode") ?: "history"
                LibraryScreen(navController, mode)
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

/**
 * Bottom nav meniru web (BottomNav.tsx): 68dp, latar --surface, garis atas
 * 1px --line, label hanya pada item aktif, item portal mengikuti preferensi
 * (Anime ATAU Donghua), item profil memakai avatar bila tersedia.
 */
@Composable
private fun CyBottomBar(
    current: String?,
    portalAnime: Boolean,
    me: Me?,
    onTab: (String) -> Unit
) {
    Column(Modifier.background(Cy.Surface)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Cy.Line)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            BottomItem("home", "Home", Icons.Filled.Home, current == "home", Modifier.weight(1f), onTab)
            // Portal (Anime / Donghua sesuai preferensi)
            BottomItem(
                "portal",
                if (portalAnime) "Anime" else "Donghua",
                if (portalAnime) Icons.Filled.LiveTv else Icons.Filled.AutoAwesome,
                current == "portal/{type}",
                Modifier.weight(1f),
                onTab
            )
            // Cari
            BottomItem("search", "Cari", Icons.Filled.Search, current == "search", Modifier.weight(1f), onTab)
            // Profil (avatar bila ada, ala web)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTab("profile") },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val active = current == "profile"
                if (!me?.image.isNullOrBlank()) {
                    AsyncImage(
                        model = me!!.image,
                        contentDescription = "Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(Cy.RadiusChip))
                            .background(Cy.Surface2)
                    )
                } else {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(Cy.RadiusChip))
                            .background(Cy.Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person, "Profil",
                            tint = if (active) Cy.Text else Cy.Text2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (active) {
                    Text(
                        "Profil",
                        color = Cy.Text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomItem(
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onTab: (String) -> Unit
) {
    Column(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTab(route) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon, label,
            tint = if (active) Cy.Text else Cy.Text2,
            modifier = Modifier.size(24.dp)
        )
        if (active) {
            Text(
                label,
                color = Cy.Text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
