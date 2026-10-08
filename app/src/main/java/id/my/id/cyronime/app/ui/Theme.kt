package id.my.id.cyronime.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Token desain SAMA dengan Web (globals.css, tema NAVY):
 * warna datar, tanpa gradien/glow; kedalaman = beda surface + garis 1px
 * lavender opasitas rendah. Aksen #6A69F3 hanya untuk aksi utama / state aktif.
 */
object Cy {
    val Navy = Color(0xFF212237)        // --navy / --page-bg
    val Surface = Color(0xFF2A2B46)     // --surface (kartu/panel)
    val Surface2 = Color(0xFF34365A)    // --surface-2
    val Accent = Color(0xFF6A69F3)      // --accent
    val Lavender = Color(0xFF7969B0)    // --lavender
    val Text = Color(0xFFFEFDFF)        // --text
    val Text2 = Color(0xFFB9B2E0)       // --text-2
    val Peach = Color(0xFFF5C5B7)       // --peach
    val Line = Color(0x477969B0)        // --line (28%)
    val LineStrong = Color(0x807969B0)  // --line-strong (50%)
    val Overlay = Color(0xE0212237)     // --overlay (chip di atas poster)

    val RadiusCard = 18.dp
    val RadiusMd = 12.dp
    val RadiusChip = 8.dp
}

private val CyronimeColors = darkColorScheme(
    primary = Cy.Accent,
    onPrimary = Cy.Text,
    background = Cy.Navy,
    onBackground = Cy.Text,
    surface = Cy.Navy,
    onSurface = Cy.Text,
    surfaceVariant = Cy.Surface,
    onSurfaceVariant = Cy.Text2,
    surfaceContainer = Cy.Surface,
    surfaceContainerHigh = Cy.Surface2,
    secondary = Cy.Lavender,
    tertiary = Cy.Peach,
    error = Cy.Peach,
    outline = Cy.Line,
    outlineVariant = Cy.Line
)

private val CyShapes = Shapes(
    small = RoundedCornerShape(Cy.RadiusChip),
    medium = RoundedCornerShape(Cy.RadiusMd),
    large = RoundedCornerShape(Cy.RadiusCard)
)

@Composable
fun CyronimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CyronimeColors, shapes = CyShapes, content = content)
}
