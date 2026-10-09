package id.my.id.cyronime.app.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import id.my.id.cyronime.app.R
import kotlinx.coroutines.delay

private enum class Step { Splash, Disclaimer, Declined, Intro, Carousel, Pick }

private class Slide(val title: String, val desc: String, val art: Int)

private val SLIDES = listOf(
    Slide("Lanjut dari terakhir kali",
        "Episode yang kamu tonton tercatat otomatis. Buka lagi kapan saja dan lanjut dari detik terakhir.",
        R.drawable.onb_feature_resume),
    Slide("Simpan serial favorit",
        "Subscribe Series menyimpan serial yang kamu ikuti, episode barunya terkumpul di satu daftar.",
        R.drawable.onb_feature_subscribe),
    Slide("Anime dan donghua, satu tempat",
        "Pilih tontonan utama kamu di langkah berikutnya. Bisa diganti kapan saja dari tombol portal.",
        R.drawable.onb_feature_portals)
)

/**
 * Onboarding NATIVE yang meniru alur Web (OnboardingFlow.tsx):
 * Splash -> Disclaimer -> Intro Airin -> Carousel -> [Login] -> Pilih Tontonan.
 * Login disisipkan oleh pemanggil lewat [loginContent] sebelum "Pilih Tontonan",
 * sama seperti Web (login WAJIB, tidak ada mode tamu).
 */
@Composable
fun OnboardingScreen(
    loggedIn: Boolean,
    loginContent: @Composable (onLoggedIn: () -> Unit) -> Unit,
    onPick: (portal: String) -> Unit
) {
    // Sudah login (mis. sesi lama) tapi belum pernah memilih portal -> langsung Pilih Tontonan.
    var step by remember { mutableStateOf(if (loggedIn) Step.Pick else Step.Splash) }
    var slide by remember { mutableIntStateOf(0) }
    var needLogin by remember { mutableStateOf(false) }
    var justLoggedIn by remember { mutableStateOf(false) }
    val isIn = loggedIn || justLoggedIn

    LaunchedEffect(step) {
        if (step == Step.Splash) { delay(1800); step = Step.Disclaimer }
    }

    BackHandler(enabled = step != Step.Splash && step != Step.Disclaimer) {
        when (step) {
            Step.Carousel -> if (slide > 0) slide-- else step = Step.Intro
            Step.Intro -> step = Step.Disclaimer
            Step.Declined -> step = Step.Disclaimer
            Step.Pick -> if (!isIn) needLogin = true
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize().background(Cy.Navy)) {
        if (needLogin && !isIn) {
            loginContent { justLoggedIn = true; needLogin = false; step = Step.Pick }
            return@Box
        }
        AnimatedContent(targetState = step, label = "onb") { s ->
            when (s) {
                Step.Splash -> Splash()
                Step.Disclaimer -> Disclaimer(
                    onAccept = { step = Step.Intro },
                    onDecline = { step = Step.Declined }
                )
                Step.Declined -> Declined { step = Step.Disclaimer }
                Step.Intro -> Intro { step = Step.Carousel; slide = 0 }
                Step.Carousel -> Carousel(
                    index = slide,
                    onBack = { if (slide > 0) slide-- else step = Step.Intro },
                    onNext = {
                        if (slide < SLIDES.lastIndex) slide++
                        else if (isIn) step = Step.Pick else needLogin = true
                    },
                    onSkip = { if (isIn) step = Step.Pick else needLogin = true },
                    onDot = { slide = it }
                )
                Step.Pick -> Pick(onPick)
            }
        }
    }
}

@Composable
private fun Mascot(size: Int) {
    Image(
        painter = painterResource(R.drawable.airin),
        contentDescription = "Airin, maskot Cyronime",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size.dp)
    )
}

@Composable
private fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(Cy.RadiusMd))
            .background(Cy.Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Cy.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Splash() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Mascot(160)
        Spacer(Modifier.height(28.dp))
        Text("Selamat datang di senja", color = Cy.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { Box(Modifier.size(8.dp).clip(CircleShape).background(Cy.Lavender)) }
        }
    }
}

@Composable
private fun Disclaimer(onAccept: () -> Unit, onDecline: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(Cy.RadiusCard))
                .background(Cy.Surface)
                .padding(24.dp)
        ) {
            Text("Disclaimer", color = Cy.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(
                "Cyronime adalah situs streaming TIDAK RESMI dan tidak berafiliasi dengan studio, " +
                    "penerbit, atau pemegang lisensi mana pun. Seluruh hak cipta konten (anime, donghua, " +
                    "gambar, dan judul) tetap menjadi milik pemiliknya masing-masing. Lanjutkan hanya jika " +
                    "kamu memahami dan menyetujui hal ini.",
                color = Cy.Text2, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Setuju, lanjut", Modifier.fillMaxWidth(), onAccept)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(Cy.RadiusMd))
                    .background(Cy.Surface2)
                    .clickable(onClick = onDecline),
                contentAlignment = Alignment.Center
            ) { Text("Nggak dulu", color = Cy.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Declined(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Mascot(150)
        Spacer(Modifier.height(20.dp))
        Text("Belum bisa lanjut", color = Cy.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Cyronime hanya bisa dipakai kalau kamu menyetujui disclaimernya. Baca sekali lagi, " +
                "kamu bisa setuju di bawahnya.",
            color = Cy.Text2, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Baca lagi disclaimernya", onClick = onBack)
    }
}

@Composable
private fun Intro(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Mascot(260)
        Spacer(Modifier.height(32.dp))
        Text("Halo, aku Airin!", color = Cy.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Aku bakal nemenin kamu jelajahi anime dan donghua subtitle Indonesia di sini. " +
                "Yuk kenalan dulu sama cara pakainya.",
            color = Cy.Text2, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Kenalan dulu", onClick = onStart)
    }
}

@Composable
private fun Carousel(
    index: Int, onBack: () -> Unit, onNext: () -> Unit, onSkip: () -> Unit, onDot: (Int) -> Unit
) {
    val s = SLIDES[index]
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Cy.Text2,
                modifier = Modifier.size(26.dp).clickable(onClick = onBack))
            Text("Skip", color = Cy.Text2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onSkip))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(s.art), contentDescription = s.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(280.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(32.dp))
            Text(s.title, color = Cy.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(s.desc, color = Cy.Text2, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SLIDES.indices.forEach { i ->
                    Box(
                        Modifier.size(if (i == index) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (i == index) Cy.Accent else Color(0xFF6051AE))
                            .clickable { onDot(i) }
                    )
                }
            }
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Cy.Accent).clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Lanjut", tint = Cy.Text) }
        }
    }
}

@Composable
private fun Pick(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pilih tontonan kamu", color = Cy.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Bisa diganti kapan saja dari tombol portal.", color = Cy.Text2, fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
        PickCard(Icons.Filled.LiveTv, "Anime", "Serial Jepang, sub Indonesia") { onPick("anime") }
        Spacer(Modifier.height(14.dp))
        PickCard(Icons.Filled.AutoAwesome, "Donghua", "Serial China, sub Indonesia") { onPick("donghua") }
    }
}

@Composable
private fun PickCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Cy.RadiusCard))
            .background(Cy.Surface)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(Cy.RadiusMd)).background(Cy.Surface2),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Cy.Text, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.size(16.dp))
        Column {
            Text(title, color = Cy.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Cy.Text2, fontSize = 13.sp)
        }
    }
}
