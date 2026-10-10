package id.my.id.cyronime.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import id.my.id.cyronime.app.AppSettings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Token desain SAMA dengan Web (globals.css, tema NAVY):
 * warna datar, tanpa gradien/glow; kedalaman = beda surface + garis 1px
 * lavender opasitas rendah. Aksen #6A69F3 hanya untuk aksi utama / state aktif.
 * Overlay di atas poster selalu gelap (kelas .on-media di Web).
 */
object Cy {
    /** True bila tema TERANG sedang aktif (diatur oleh [CyronimeTheme]). */
    var isLight by mutableStateOf(false)
        internal set

    /** Aksen aktif (state Compose: ganti aksen langsung memperbarui seluruh UI). */
    var accentColor by mutableStateOf(Color(0xFF6A69F3))
        internal set

    // ---- Palet GELAP asli (identik dengan Web, tidak diubah) ----
    private val DNavy = Color(0xFF212237)
    private val DSurface = Color(0xFF2A2B46)
    private val DSurface2 = Color(0xFF34365A)
    private val DText = Color(0xFFFEFDFF)
    private val DText2 = Color(0xFFB9B2E0)
    private val DLine = Color(0x477969B0)
    private val DLineStrong = Color(0x807969B0)

    // ---- Palet TERANG: lavender-putih senada identitas Cyronime ----
    private val LNavy = Color(0xFFF4F3FB)
    private val LSurface = Color(0xFFFFFFFF)
    private val LSurface2 = Color(0xFFE9E7F7)
    private val LText = Color(0xFF1C1D33)
    private val LText2 = Color(0xFF5B5784)
    private val LLine = Color(0x337969B0)
    private val LLineStrong = Color(0x667969B0)

    val Navy get() = if (isLight) LNavy else DNavy
    val Surface get() = if (isLight) LSurface else DSurface
    val Surface2 get() = if (isLight) LSurface2 else DSurface2
    val Surface3 get() = Surface2
    val Accent get() = accentColor
    val Lavender = Color(0xFF7969B0)
    val DeepPurple = Color(0xFF6051AE)
    val Text get() = if (isLight) LText else DText
    val Text2 get() = if (isLight) LText2 else DText2
    val Peach = Color(0xFFF5C5B7)
    val Line get() = if (isLight) LLine else DLine
    val LineStrong get() = if (isLight) LLineStrong else DLineStrong

    // ---- Selalu GELAP: player, overlay di atas poster (aturan sama dengan Web .on-media) ----
    /** Teks/ikon di atas overlay/poster/player — selalu terang agar terbaca di atas gelap. */
    val OnMedia = DText
    val OnMedia2 = DText2
    val Overlay = Color(0xE0212237)
    val OverlayStrong = Color(0xEB212237)
    val OverlaySoft = Color(0xB8212237)
    val Scrim = Color(0xB8212237)

    val RadiusCard = 18.dp    // --radius-card
    val RadiusApp = 16.dp    // rounded-app (ContinueWatchingCard 130x76)
    val RadiusMd = 12.dp      // --radius-md
    val RadiusChip = 8.dp     // --radius-chip
    val RadiusPlayer = 14.dp  // kartu strip episode 72x56 (radius 14)

    /** Gradasi senja di bawah hero Web: transparan -> .6 -> .96 warna latar halaman (ikut tema). */
    val HeroScrimBottom get() = Navy.copy(alpha = 0.96f)
    val HeroScrimMid get() = Navy.copy(alpha = 0.6f)
    val HeroScrimTop get() = Navy.copy(alpha = 0f)
}

private val CyShapes = Shapes(
    small = RoundedCornerShape(Cy.RadiusChip),
    medium = RoundedCornerShape(Cy.RadiusMd),
    large = RoundedCornerShape(Cy.RadiusCard)
)

/** Aksen -> warna. Ungu = default Web; biru & merah muda tetap selaras dengan lavender Cyronime. */
private fun accentOf(a: AppSettings.Accent, light: Boolean): Color = when (a) {
    AppSettings.Accent.Purple -> Color(0xFF6A69F3)
    AppSettings.Accent.Blue -> if (light) Color(0xFF2F7DE1) else Color(0xFF4C9BFF)
    AppSettings.Accent.Pink -> if (light) Color(0xFFD6459C) else Color(0xFFF06EB6)
}

@Composable
fun CyronimeTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val light = when (AppSettings.themeMode) {
        AppSettings.ThemeMode.Light -> true
        AppSettings.ThemeMode.Dark -> false
        AppSettings.ThemeMode.System -> !systemDark
    }
    // Publikasikan ke Cy SEBELUM konten dikomposisi: semua Cy.* ikut berganti.
    Cy.isLight = light
    Cy.accentColor = accentOf(AppSettings.accent, light)

    val colors = if (light) {
        lightColorScheme(
            primary = Cy.Accent, onPrimary = Color.White,
            background = Cy.Navy, onBackground = Cy.Text,
            surface = Cy.Navy, onSurface = Cy.Text,
            surfaceVariant = Cy.Surface, onSurfaceVariant = Cy.Text2,
            surfaceContainer = Cy.Surface, surfaceContainerHigh = Cy.Surface2,
            secondary = Cy.Lavender, tertiary = Cy.Peach, error = Cy.Peach,
            outline = Cy.Line, outlineVariant = Cy.Line
        )
    } else {
        darkColorScheme(
            primary = Cy.Accent, onPrimary = Cy.Text,
            background = Cy.Navy, onBackground = Cy.Text,
            surface = Cy.Navy, onSurface = Cy.Text,
            surfaceVariant = Cy.Surface, onSurfaceVariant = Cy.Text2,
            surfaceContainer = Cy.Surface, surfaceContainerHigh = Cy.Surface2,
            secondary = Cy.Lavender, tertiary = Cy.Peach, error = Cy.Peach,
            outline = Cy.Line, outlineVariant = Cy.Line
        )
    }

    // Ukuran teks: skala fontScale (dp tetap) -> hanya teks yang membesar/mengecil.
    val base = LocalDensity.current
    val density = Density(base.density, base.fontScale * AppSettings.textScale.scale)
    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(colorScheme = colors, shapes = CyShapes, content = content)
    }
}
