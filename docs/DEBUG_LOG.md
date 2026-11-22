# DEBUG_LOG — Gemini Neon Self-Healing & Static Verification Log

## Iterasi 0 — Analisis Statis & Preflight (Mode B, 22 November 2026)

- **Lingkungan:** Linux x86_64, OpenJDK 17.0.21 JRE (tanpa `javac`, tanpa JDK 25, tanpa Gradle, offline).
- **Artefak Input:** `The-Hordes-1.21.1-1.6.3f.jar` (SHA-256 `4f4e308d4a412ffd9a2eee71bfb7cf729c306524ccac6b61ab4dcbca648a8574`).
- **Temuan Koreksi API Paper 26.3 Sebelum Kompilasi:**
  1. `Attribute.GENERIC_*` dihapus sejak Paper 1.21.3+/26.3 → seluruh kode menggunakan `Attribute.MAX_HEALTH`, `Attribute.FOLLOW_RANGE`, dan `Attribute.MOVEMENT_SPEED` (diverifikasi di [Paper 26.3 Attribute Javadoc](https://jd.papermc.io/paper/26.3/org/bukkit/attribute/Attribute.html)).
  2. `paper-plugin.yml` pada Paper 26.3 tidak memakai blok `commands:` → seluruh 9 subperintah `/hordes` didaftarkan lewat `LifecycleEvents.COMMANDS` + Brigadier `Commands.literal()` + `ArgumentTypes.players()` ([Paper Command API](https://docs.papermc.io/paper/dev/command-api/basics/registration/)).
  3. `paper-api` 26.3 sudah berstatus stabil (`26.3.build.255-stable`) → disematkan di `gradle.properties`.

## Panduan Iterasi Self-Healing (Human-in-the-Loop Mode B)

Saat Anda menjalankan `scripts/run-test-server.sh` di mesin lokal dengan JDK 25 & Gradle:
1. Jika muncul error kompilasi Gradle, tempelkan output `gradle build --stacktrace` ke chat untuk patch minimal terarah.
2. Jika plugin berhasil dimuat di Paper 26.3, jalankan `neontest run ALL` di konsol server dan tempelkan isi `plugins/NeonTestKit/results.log`.
