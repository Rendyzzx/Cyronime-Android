package id.my.id.cyronime.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Pengaturan personalisasi pengguna (tema, aksen, tampilan, bahasa, pemutaran).
 *
 * Disimpan LOKAL di SharedPreferences "cyronime" (file yang sama dengan [Prefs])
 * karena backend tidak punya endpoint untuk preferensi tampilan. Preferensi
 * notifikasi TIDAK di sini: tetap di backend (sinkron Web/Android).
 *
 * Setiap properti berbasis Compose state: mengubah nilai langsung memperbarui UI
 * yang membacanya, tanpa restart Activity.
 */
object AppSettings {
    private const val FILE = "cyronime"

    enum class ThemeMode(val key: String) { System("system"), Dark("dark"), Light("light") }
    enum class Accent(val key: String) { Purple("purple"), Blue("blue"), Pink("pink") }
    enum class TextScale(val key: String, val scale: Float) {
        Small("small", 0.9f), Normal("normal", 1.0f), Large("large", 1.15f)
    }
    enum class PosterRatio(val key: String, val ratio: Float) {
        Portrait("portrait", 3f / 4f), Tall("tall", 2f / 3f)
    }
    enum class Language(val key: String) { Id("id"), En("en") }
    /** Kualitas pilihan; Auto = biarkan player memilih server terbaik. */
    enum class Quality(val key: String, val match: String?) {
        Auto("auto", null), Q360("360", "360"), Q480("480", "480"), Q720("720", "720")
    }

    var themeMode by mutableStateOf(ThemeMode.System); private set
    var accent by mutableStateOf(Accent.Purple); private set
    var textScale by mutableStateOf(TextScale.Normal); private set
    /** Jumlah kolom grid poster: 2 (besar) atau 3 (padat, default sebelumnya). */
    var gridColumns by mutableStateOf(3); private set
    var posterRatio by mutableStateOf(PosterRatio.Portrait); private set
    var language by mutableStateOf(Language.Id); private set

    var quality by mutableStateOf(Quality.Auto); private set
    var autoPlayNext by mutableStateOf(true); private set
    var autoPlay by mutableStateOf(true); private set
    var rememberPosition by mutableStateOf(true); private set

    private var loaded = false

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Muat sekali saat Activity dibuat. Nilai tak dikenal -> default (tak pernah mereset yang valid). */
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = sp(ctx)
        themeMode = ThemeMode.entries.firstOrNull { it.key == p.getString("set_theme", null) } ?: ThemeMode.System
        accent = Accent.entries.firstOrNull { it.key == p.getString("set_accent", null) } ?: Accent.Purple
        textScale = TextScale.entries.firstOrNull { it.key == p.getString("set_text", null) } ?: TextScale.Normal
        gridColumns = p.getInt("set_cols", 3).let { if (it == 2) 2 else 3 }
        posterRatio = PosterRatio.entries.firstOrNull { it.key == p.getString("set_ratio", null) } ?: PosterRatio.Portrait
        language = p.getString("set_lang", null)
            ?.let { k -> Language.entries.firstOrNull { it.key == k } }
            ?: if (java.util.Locale.getDefault().language == "en") Language.En else Language.Id
        quality = Quality.entries.firstOrNull { it.key == p.getString("set_quality", null) } ?: Quality.Auto
        autoPlayNext = p.getBoolean("set_autonext", true)
        autoPlay = p.getBoolean("set_autoplay", true)
        rememberPosition = p.getBoolean("set_rempos", true)
    }

    fun setThemeMode(ctx: Context, v: ThemeMode) { themeMode = v; sp(ctx).edit().putString("set_theme", v.key).apply() }
    fun setAccent(ctx: Context, v: Accent) { accent = v; sp(ctx).edit().putString("set_accent", v.key).apply() }
    fun setTextScale(ctx: Context, v: TextScale) { textScale = v; sp(ctx).edit().putString("set_text", v.key).apply() }
    fun setGridColumns(ctx: Context, v: Int) {
        gridColumns = if (v == 2) 2 else 3
        sp(ctx).edit().putInt("set_cols", gridColumns).apply()
    }
    fun setPosterRatio(ctx: Context, v: PosterRatio) { posterRatio = v; sp(ctx).edit().putString("set_ratio", v.key).apply() }
    fun setLanguage(ctx: Context, v: Language) { language = v; sp(ctx).edit().putString("set_lang", v.key).apply() }
    fun setQuality(ctx: Context, v: Quality) { quality = v; sp(ctx).edit().putString("set_quality", v.key).apply() }
    fun setAutoPlayNext(ctx: Context, v: Boolean) { autoPlayNext = v; sp(ctx).edit().putBoolean("set_autonext", v).apply() }
    fun setAutoPlay(ctx: Context, v: Boolean) { autoPlay = v; sp(ctx).edit().putBoolean("set_autoplay", v).apply() }
    fun setRememberPosition(ctx: Context, v: Boolean) { rememberPosition = v; sp(ctx).edit().putBoolean("set_rempos", v).apply() }
}
