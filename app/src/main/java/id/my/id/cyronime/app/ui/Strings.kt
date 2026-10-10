package id.my.id.cyronime.app.ui

import id.my.id.cyronime.app.AppSettings

/**
 * Teks antarmuka dwibahasa. Membaca [AppSettings.language] (state Compose),
 * sehingga mengganti bahasa langsung memperbarui layar yang sedang tampil.
 *
 * Pakai: `tr("Cari", "Search")` — argumen pertama Indonesia, kedua English.
 * Teks yang berisi data (judul anime, nama, angka) tidak ikut diterjemahkan.
 */
fun tr(id: String, en: String): String =
    if (AppSettings.language == AppSettings.Language.En) en else id
