# Pemetaan Konfigurasi — The Hordes (NeoForge TOML/JSON) → HordesPaper (YAML)

Seluruh kunci konfigurasi di bawah diverifikasi langsung dari bytecode `HordeEventConfig.class`, `InfectionConfig.class`, `ZombiePlayersConfig.class`, `CommonConfigHandler.class`, dan `config_defaults/data/hordes/**` pada `The-Hordes-1.21.1-1.6.3f.jar`.

## 1. Horde Event (`hordes-common.toml [Horde Event]` → `config.yml -> horde_event`)

| Opsi Mod (NeoForge) | Kunci Plugin (`config.yml`) | Default | Keterangan |
| --- | --- | --- | --- |
| `enableHordeEvent` | `horde_event.enabled` | `true` | Mengaktifkan/menonaktifkan seluruh sistem horde |
| `hordesCommandOnly` | `horde_event.command_only` | `false` | Jika `true`, horde hanya dapat dimulai lewat `/hordes start` |
| `spawnAmount` | `horde_event.spawn_amount` | `10` | Jumlah mob per gelombang |
| `hordeSpawnMultiplier` | `horde_event.spawn_multiplier` | `1.05` | Faktor pengganda jumlah mob tiap horde alami (set `1.0` untuk nonaktif) |
| `hordeSpawnMax` | `horde_event.spawn_max` | `60` | Batas maksimal entitas horde aktif bersamaan per pemain (tidak menghitung passenger) |
| `hordeSpawnDuration` | `horde_event.spawn_duration` | `6000` | Durasi event horde dalam tick (`6000` = tengah malam 18000 s.d. pagi 24000) |
| `hordeSpawnInterval` | `horde_event.spawn_interval` | `1000` | Jeda antar gelombang dalam tick |
| `hordeStartTime` | `horde_event.start_time` | `18000` | Waktu hari Minecraft saat horde dimulai (`18000` = tengah malam) |
| `hordeSpawnDays` | `horde_event.spawn_days` | `10` | Interval hari antar malam horde |
| `hordeSpawnVariation` | `horde_event.spawn_variation` | `0` | Variasi acak tambahan hari (0 s.d. N hari) |
| `dayLength` | `horde_event.day_length` | `24000` | Panjang 1 hari Minecraft dalam tick |
| `hordePathingInterval` | `horde_event.pathing_interval` | `60` | Interval tick re-targeting AI mob horde ke pemain |
| `hordeEntitySpeed` | `horde_event.entity_speed` | `1.0` | Pengali kecepatan gerak mob horde |
| `spawnFirstDay` | `horde_event.spawn_first_day` | `false` | Memicu horde pada hari ke-0 |
| `canSleepDuringHorde` | `horde_event.can_sleep_during_horde` | `false` | Mengizinkan/melarang tidur saat horde aktif |
| `hordePreventsOtherPlayersSleeping` | `horde_event.prevents_other_players_sleeping` | `true` | Horde milik satu pemain mencegah pemain lain tidur |
| `hordeMultiplayerScaling` | `horde_event.multiplayer_scaling` | `0.8` | Faktor pengurangan jumlah mob ketika beberapa pemain berdekatan |
| `pauseEventServer` | `horde_event.pause_when_empty` | `true` | Menjeda siklus horde saat tidak ada pemain online |
| `hordeEventByPlayerTime` | `horde_event.track_by_player_time` | `false` | Melacak hari horde per waktu bermain pemain (bukan waktu dunia) |
| `hordeStartBuffer` | `horde_event.start_buffer` | `200` | Toleransi tick setelah `start_time` agar horde tetap dapat dimulai |
| `hordeSpawnChecks` | `horde_event.spawn_checks` | `10` | Jumlah percobaan mencari lokasi spawn gelap & valid sebelum fallback |
| `hordeSpawnDistance` | `horde_event.spawn_distance` | `75.0` | Jarak maksimal spawn & follow range mob horde |
| *(Skrip `add_reward_command`)* | `horde_event.reward_commands` | `[]` | Daftar perintah konsol saat horde berakhir (mendukung `%player%`, `%world%`, `%day%`) |

## 2. Infeksi (`hordes-common.toml [Infection]` → `config.yml -> infection` & `infection.yml`)

| Opsi Mod (NeoForge) | Kunci Plugin | Default | Keterangan |
| --- | --- | --- | --- |
| `enableMobInfection` | `infection.enabled` | `true` | Mengaktifkan sistem infeksi |
| `infectPlayers` | `infection.infect_players` | `true` | Pemain dapat terinfeksi |
| `playerInfectionResistance` | `infection.player_base_resistance` | `0.25` | Resistensi dasar pemain (0.0–1.0) |
| `ticksForEffectStage` | `infection.ticks_per_stage` | `1500` | Durasi tiap tahap (4 tahap x 1500 = 6000 tick = 5 menit) |
| `infectSlowness` | `infection.apply_slowness` | `true` | Memberikan Slowness pada tahap lanjut (tahap 3–4) |
| `infectHunger` | `infection.apply_hunger` | `true` | Mempercepat lapar pada tahap lanjut (tahap 2–4) |
| `infectionSpawnsZombiePlayers` | `infection.spawn_zombie_players` | `true` | Pemain mati terinfeksi memunculkan Zombie Player |
| `infectionEntitiesAggroConversions` | `infection.aggro_conversions` | `true` | Mob penginfeksi menarget entitas yang bisa dikonversi |
| `effectStageTickReduction` | `infection.stage_tick_reduction` | `0.95` | Pengali durasi tahap setiap kali pemain sembuh (reset saat mati) |
| `infection_entities.json` | `infection.yml -> infecting_entities` | 10 entitas | Peluang infeksi per tipe mob penyerang |
| `infection_conversions.json` | `infection.yml -> conversions` | 11 entitas | Konversi mob non-pemain + proteksi & HP varian |
| `wearables_protection.json` | `infection.yml -> wearable_protection` | 30 item | Resistensi infeksi armor pemain & horse/wolf armor |
| `infection_cures.json` & `immunity_items.json` | `infection.yml -> cures` & `immunity_items` | Golden Apple & Enchanted Golden Apple (150s) | Item penyembuh dan pemberi imunitas |

## 3. Zombie Players (`hordes-common.toml [Zombie Players]` → `config.yml -> zombie_players`)

| Opsi Mod (NeoForge) | Kunci Plugin (`config.yml`) | Default | Keterangan |
| --- | --- | --- | --- |
| `zombieGraves` | `zombie_players.zombie_graves` | `false` | Selalu spawn Zombie Player saat mati (bahkan tanpa infeksi) |
| `drownedPlayers` | `zombie_players.drowned_players` | `true` | Spawn Drowned jika pemain mati di dalam air |
| `huskPlayers` | `zombie_players.husk_players` | `true` | Spawn Husk jika pemain mati di desert/savanna/badlands |
| `zombiePlayersFireImmune` | `zombie_players.fire_immune` | `false` | Kebal damage api/lava |
| `zombiePlayersBurn` | `zombie_players.burn_in_sunlight` | `false` | Terbakar di bawah sinar matahari |
| `zombiePlayersOnlyHurtByPlayers` | `zombie_players.only_hurt_by_players` | `false` | Kebal damage selain dari pemain |
| `zombiePlayersStoreItems` | `zombie_players.store_items` | `true` | Menyimpan & menjatuhkan seluruh inventaris pemain saat dibunuh |
| `zombiePlayersDespawnPeaceful` | `zombie_players.despawn_peaceful` | `false` | Mempertahankan entitas di mode peaceful |

## 4. Opsi Misc (`hordes-common.toml [Misc]` → `config.yml -> misc`)

| Opsi Mod (NeoForge) | Kunci Plugin (`config.yml`) | Default |
| --- | --- | --- |
| `zombiesBurn` | `misc.zombies_burn` | `true` |
| `skeletonsBurn` | `misc.skeletons_burn` | `true` |
| `zombieVillagersCanBeCured` | `misc.zombie_villagers_can_be_cured` | `true` |
| `piglinsHoglinsConvert` | `misc.piglins_hoglins_convert` | `true` |
| `aggressiveZombieHorses` | `misc.aggressive_zombie_horses` | `true` |
| `zombieHorsesBurn` | `misc.zombie_horses_burn` | `false` |
| `skeletonHorsesBurn` | `misc.skeleton_horses_burn` | `false` |
| `zombiesScareHorses` | `misc.zombies_scare_horses` | `true` |
| `zombiePiglinsHostile` | `misc.zombie_piglins_hostile` | `false` |
| `piglinsHuntZombies` | `misc.piglins_hunt_zombies` | `true` |
| `piglinsCureThemself` | `misc.piglins_cure_themselves` | `true` |
| `illagersHuntZombies` | `misc.illagers_hunt_zombies` | `true` |
| `zoglinsAttackUndead` | `misc.zoglins_attack_undead` | `false` |
| `zoglinsAttackMobs` | `misc.zoglins_attack_mobs` | `true` |
