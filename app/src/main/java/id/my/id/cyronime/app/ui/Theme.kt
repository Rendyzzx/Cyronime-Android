package id.my.id.cyronime.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF121316)
private val NavySurface = Color(0xFF212237)
private val NavySurface2 = Color(0xFF2A2B44)
private val Accent = Color(0xFF6A69F3)
private val TextMain = Color(0xFFE6E4F3)
private val TextSecond = Color(0xFF9A99B5)
private val Peach = Color(0xFFE58B8B)

private val CyronimeColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Navy,
    onBackground = TextMain,
    surface = NavySurface,
    onSurface = TextMain,
    surfaceVariant = NavySurface2,
    onSurfaceVariant = TextSecond,
    secondary = Accent,
    error = Peach,
    outline = Color(0xFF3A3B58)
)

/** Tema app — dark navy senada Web (dark mode Web adalah default Cyronime). */
@Composable
fun CyronimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyronimeColors,
        content = content
    )
}
