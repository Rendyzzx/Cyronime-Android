# Aturan minify untuk release build (R8 + shrinkResources).
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# OkHttp / Coil / Media3: sebagian diakses via refleksi (OkHttp internally,
# media3 datasource registry). Baris dontwarn cukup; aturan consumer rules
# dari AAR sudah menangani keep yang benar.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Model data di-parse manual dari JSONObject — field-nya aman (tidak via
# refleksi), tapi kelas model tetap di-keep supaya stack trace mudah dibaca.
-keep class id.my.id.cyronime.app.data.** { *; }
-keep class id.my.id.cyronime.app.BuildConfig { *; }
