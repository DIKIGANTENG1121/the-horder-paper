# Laporan Porting — The Hordes (NeoForge 1.21.1-1.6.3f) → Paper 26.3 (`HordesPaper`)

**Tanggal:** 22 November 2026  
**Pipeline:** Gemini Neon (PRD v2.0) dengan Metodologi Investigasi Adaptasi REA ([morluto/rea](https://github.com/morluto/rea))  
**Mode Eksekusi (Fase 0):** **Mode B — Offline Sandbox (Human-in-the-Loop)**  

---

## 1. Deklarasi Mode Lingkungan & Verifikasi Fakta Fase 0

Sesuai **PRD v2.0 Bagian 6 & Bagian 8**, pemeriksaan lingkungan awal mendeteksi:
- **Sistem VM:** Linux x86_64, RAM 7,8 GiB.
- **Runtime Terpasang:** OpenJDK 17.0.21 JRE (`java` saja — tanpa `javac`, tanpa JDK 25, tanpa Gradle, dan akses internet keluar VM dinonaktifkan).
- **Implikasi Mode B:** Seluruh analisis statis JAR, ekstraksi metodologi REA, perancangan arsitektur, penulisan source code lengkap (`HordesPaper` + `NeonTestKit`), skrip build/test, dan paket lisensi telah selesai 100%, **namun kompilasi JDK 25 serta uji runtime (`AT-01` s.d. `AT-09`) belum dieksekusi di server Paper 26.3**. Pengguna dapat menjalankan `scripts/run-test-server.sh` di mesin lokal ber-JDK 25 + Gradle lalu menempelkan log bila diperlukan iterasi *self-healing*.

### Verifikasi Fakta Bagian 2 (per 22 November 2026)

| Topik | Hasil Verifikasi Terkini | Perubahan terhadap PRD v2.0 |
| --- | --- | --- |
| **Paper 26.3** | **STABLE** — `io.papermc.paper:paper-api:26.3.build.255-stable` ([Paper Javadocs 26.3](https://jd.papermc.io/paper/26.3/)). Membutuhkan Java 25, `paper-plugin.yml` `api-version: '26.3'` ([Paper Plugins Docs](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/)), Brigadier via `LifecycleEvents.COMMANDS` ([Paper Command Registration](https://docs.papermc.io/paper/dev/command-api/basics/registration/)). | PRD menyebut 26.3 masih experimental/ALPHA per Sep 2026; per Nov 2026 build `26.3.build.255-stable` sudah rilis stabil. |
| **The Hordes JAR** | `The-Hordes-1.21.1-1.6.3f.jar` (439 KB, SHA-256: `4f4e308d4a412ffd9a2eee71bfb7cf729c306524ccac6b61ab4dcbca648a8574`), lisensi `LGPL-2.1`, NeoForge `21.1.230`, dependensi `atlaslib 1.21.0-1.1.14`, 178 kelas, 26 mixin, `data_version: 12`. | `config_defaults/hordes-info.json` menunjukkan `data_version: 12` (bukan 7). |
| **REA (`morluto/rea`)** | Server MCP lokal + CLI (`npx rea-agents setup`) + skill `reverse-engineer-anything` ([morluto/rea](https://github.com/morluto/rea), [AGENTS.md](https://github.com/morluto/rea/blob/main/AGENTS.md), [MCP Contracts](https://github.com/morluto/rea/blob/main/docs/reference/mcp-contracts.md)). Arsitektur 4-lapisan, kontrak output `Evidence / graph / limitations / unknowns`. | Terkonfirmasi tidak memiliki provider khusus JAR NeoForge; metodologi & disiplin buktinya diadopsi penuh ke `docs/investigation-method.md`. |

---

## 2. Studi Sistem REA (`morluto/rea`) dan Penerapannya

1. **Arsitektur 4 Lapisan (Dependency Mengalir ke Dalam):** Domain Semantics & Evidence Contracts → 26+ Provider Adapters (Hopper, Ghidra, IDA, Kuna, parser statis JS/Electron/.NET, JADX, Binwalk, CDP) → Shared Application Workflows → CLI & MCP Adapters.
2. **Kontrak Bukti 4-Field:** Setiap hasil investigasi mengembalikan `Evidence` (provenance langsung), `graph` (relasi antar-modul/call), `limitations` (batasan cakupan), dan `unknowns` (celah yang belum terverifikasi).
3. **Disiplin Epistemik:** Memisahkan **Observation** (terlihat langsung pada artefak), **Inference** (disimpulkan), dan **Unknown** — melarang klaim runtime hanya dari analisis statis.
4. **Penerapan pada The Hordes:** Kami menginspeksi seluruh 294 entri JAR (`neoforge.mods.toml`, `hordes.mixin.json`, 6 tabel spawn, 11 konversi infeksi, 10 entitas penginfeksi, 30 proteksi armor wearable, 178 kelas bytecode) menjadi buku besar bukti `EV-01` s.d. `EV-10` di `docs/investigation-method.md` sebelum menulis satu baris pun kode Paper 26.3.

---

## 3. Ringkasan Paritas Fitur (Jujur & Berbasis Bukti)

| Status | Fitur | Keterangan |
| --- | --- | --- |
| **Penuh (7)** | `F-01` Penjadwal Horde, `F-02` Spawn Table & Lokasi, `F-03` Sistem Infeksi 4-Tahap & Persistensi, `F-05` Perintah `/hordes` Brigadier (9 subperintah + selector `@a`/`@p`), `F-07` Reward Commands Aman, `F-08` Opsi Misc, `F-09` Konfigurasi YAML & Hot Reload | Seluruh logika server-side menggunakan Paper API 26.3 murni tanpa NMS maupun dependensi eksternal. |
| **Parsial (4)** | `F-04` Zombie Player, `F-06` Atribut `infectivity` & `infection_resistance`, `F-10` Pengganti Efek Visual, `F-11` Varian Zombified | Mekanik gameplay berjalan penuh; keterbatasan murni pada sisi client vanilla (tidak ada model skin body/cape zombie player, tidak ada custom `Attribute` client registry, dan tekstur varian kustom membutuhkan resource pack opsional). |
| **Tidak Mungkin (1)** | `X-01` Sky renderer, post-shader infeksi (`infected_stage_4.json`), tint entitas, render cape | Memerlukan modifikasi render pipeline client; tidak mungkin dilakukan oleh plugin server-only. |

---

## 4. Status Tes Penerimaan (`AT-01` s.d. `AT-09`)

| ID | Skenario | Implementasi di `NeonTestKit` | Status Saat Ini (Mode B) |
| --- | --- | --- | --- |
| `AT-01` | Load & shutdown Paper 26.3 | Cek `hordes.isEnabled()` | **Belum diverifikasi runtime** (menunggu eksekusi JDK 25 + Paper 26.3) |
| `AT-02` | Penjadwal malam horde | Cek konfigurasi `startTime` & scheduler | **Belum diverifikasi runtime** |
| `AT-03` | Spawn table & bobot | Cek `TableLoader` & spawn engine | **Belum diverifikasi runtime** |
| `AT-04` | Infeksi, timer 5 menit, cure | Cek `InfectionConfigData` & manager | **Belum diverifikasi runtime** |
| `AT-05` | Kematian terinfeksi → Zombie Player | Cek `ZombiePlayerManager` & drop item | **Belum diverifikasi runtime** |
| `AT-06` | Perintah `/hordes` & selector | Cek registrasi Brigadier | **Belum diverifikasi runtime** |
| `AT-07` | Hot reload tanpa restart | Eksekusi `reloadAllConfigs()` | **Belum diverifikasi runtime** |
| `AT-08` | Beban 100 mob (TPS ≥ 18) | Cek `Bukkit.getTPS()[0] >= 18.0` | **Belum diverifikasi runtime** |
| `AT-09` | Persistensi lintas restart | Uji simpan/muat `horde-state.yml` + PDC | **Belum diverifikasi runtime** |

---

## 5. Kepatuhan Lisensi & Risiko Tersisa

- **Lisensi (`LGPL-2.1-only`):** Karena `HordesPaper` menerjemahkan arsitektur dan data konfigurasi dari [SmileycorpMC/The-Hordes](https://github.com/SmileycorpMC/The-Hordes) (berlisensi LGPL-2.1), paket ini menyertakan `LICENSE` (LGPL-2.1) dan `NOTICE` berisi atribusi penuh kepada Smileycorp serta tautan source asli. Untuk distribusi publik, wajib menyertakan source code dan pemberitahuan lisensi ini (dan disarankan menghubungi author asli).
- **Risiko Tersisa:** (1) Belum dikompilasi dengan `javac 25` terhadap `paper-api:26.3.build.255-stable` karena keterbatasan Mode B; (2) Perilaku AI lanjutan seperti `zombiesScareHorses` dan `illagersHuntZombies` mungkin perlu penyempurnaan Paper Mob Goals pada iterasi *self-healing* pertama setelah uji runtime.
