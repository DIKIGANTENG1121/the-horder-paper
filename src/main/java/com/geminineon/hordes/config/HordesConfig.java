package com.geminineon.hordes.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HordesConfig {

    private final JavaPlugin plugin;

    // General
    public boolean debug;

    // Horde Event (EV-03)
    public boolean hordeEnabled;
    public boolean commandOnly;
    public String defaultTable;
    public String oceanTable;
    public int spawnAmount;
    public double spawnMultiplier;
    public int spawnMax;
    public int spawnDuration;
    public int spawnInterval;
    public long startTime;
    public int spawnDays;
    public int spawnVariation;
    public long dayLength;
    public int pathingInterval;
    public double entitySpeed;
    public boolean spawnFirstDay;
    public boolean canSleepDuringHorde;
    public boolean preventsOtherPlayersSleeping;
    public double multiplayerScaling;
    public boolean pauseWhenEmpty;
    public boolean trackByPlayerTime;
    public int startBuffer;
    public int spawnChecks;
    public double spawnDistance;
    public double minSpawnDistance;
    public Set<String> enabledWorlds;
    public List<String> rewardCommands;
    public String msgEventStart;
    public String msgEventEnd;
    public String msgTrySleep;
    public String msgOtherPlayerTrySleep;

    // Infection (EV-05)
    public boolean infectionEnabled;
    public boolean infectPlayers;
    public double playerBaseResistance;
    public int ticksPerStage;
    public boolean applySlowness;
    public boolean applyHunger;
    public boolean spawnZombiePlayers;
    public boolean aggroConversions;
    public double stageTickReduction;
    public boolean showBossBar;
    public boolean showParticles;

    // Zombie Players (EV-07)
    public boolean zombieGraves;
    public boolean drownedPlayers;
    public boolean huskPlayers;
    public boolean zombiePlayersFireImmune;
    public boolean zombiePlayersBurn;
    public boolean zombiePlayersOnlyHurtByPlayers;
    public boolean zombiePlayersStoreItems;
    public boolean zombiePlayersDespawnPeaceful;
    public boolean zombiePlayersUsePlayerHead;

    // Misc (EV-08)
    public boolean zombiesBurn;
    public boolean skeletonsBurn;
    public boolean zombieVillagersCanBeCured;
    public boolean piglinsHoglinsConvert;
    public boolean aggressiveZombieHorses;
    public boolean zombieHorsesBurn;
    public boolean skeletonHorsesBurn;
    public boolean zombiesScareHorses;
    public boolean zombiePiglinsHostile;
    public boolean piglinsHuntZombies;
    public boolean piglinsCureThemselves;
    public boolean illagersHuntZombies;
    public boolean zoglinsAttackUndead;
    public boolean zoglinsAttackMobs;

    public HordesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        this.debug = c.getBoolean("debug", false);

        this.hordeEnabled = c.getBoolean("horde_event.enabled", true);
        this.commandOnly = c.getBoolean("horde_event.command_only", false);
        this.defaultTable = c.getString("horde_event.default_table", "default");
        this.oceanTable = c.getString("horde_event.ocean_table", "drowned");
        this.spawnAmount = Math.max(1, c.getInt("horde_event.spawn_amount", 10));
        this.spawnMultiplier = Math.max(1.0, c.getDouble("horde_event.spawn_multiplier", 1.05));
        this.spawnMax = Math.max(1, c.getInt("horde_event.spawn_max", 60));
        this.spawnDuration = Math.max(200, c.getInt("horde_event.spawn_duration", 6000));
        this.spawnInterval = Math.max(40, c.getInt("horde_event.spawn_interval", 1000));
        this.startTime = c.getLong("horde_event.start_time", 18000L);
        this.spawnDays = Math.max(1, c.getInt("horde_event.spawn_days", 10));
        this.spawnVariation = Math.max(0, c.getInt("horde_event.spawn_variation", 0));
        this.dayLength = Math.max(1000L, c.getLong("horde_event.day_length", 24000L));
        this.pathingInterval = Math.max(10, c.getInt("horde_event.pathing_interval", 60));
        this.entitySpeed = Math.max(0.1, c.getDouble("horde_event.entity_speed", 1.0));
        this.spawnFirstDay = c.getBoolean("horde_event.spawn_first_day", false);
        this.canSleepDuringHorde = c.getBoolean("horde_event.can_sleep_during_horde", false);
        this.preventsOtherPlayersSleeping = c.getBoolean("horde_event.prevents_other_players_sleeping", true);
        this.multiplayerScaling = c.getDouble("horde_event.multiplayer_scaling", 0.8);
        this.pauseWhenEmpty = c.getBoolean("horde_event.pause_when_empty", true);
        this.trackByPlayerTime = c.getBoolean("horde_event.track_by_player_time", false);
        this.startBuffer = Math.max(20, c.getInt("horde_event.start_buffer", 200));
        this.spawnChecks = Math.max(1, c.getInt("horde_event.spawn_checks", 10));
        this.spawnDistance = Math.max(16.0, c.getDouble("horde_event.spawn_distance", 75.0));
        this.minSpawnDistance = Math.max(8.0, c.getDouble("horde_event.min_spawn_distance", 20.0));
        this.enabledWorlds = new HashSet<>(c.getStringList("horde_event.enabled_worlds"));
        this.rewardCommands = c.getStringList("horde_event.reward_commands");
        this.msgEventStart = c.getString("horde_event.messages.event_start", "<dark_red>You hear the howls of a nearby horde</dark_red>");
        this.msgEventEnd = c.getString("horde_event.messages.event_end", "<green>The air is quiet, the horde has stopped</green>");
        this.msgTrySleep = c.getString("horde_event.messages.try_sleep", "<red>A feeling of impending dread prevents you from sleeping</red>");
        this.msgOtherPlayerTrySleep = c.getString("horde_event.messages.other_player_try_sleep", "<red>%player% is restless, it seems this night will be eventful</red>");

        this.infectionEnabled = c.getBoolean("infection.enabled", true);
        this.infectPlayers = c.getBoolean("infection.infect_players", true);
        this.playerBaseResistance = c.getDouble("infection.player_base_resistance", 0.25);
        this.ticksPerStage = Math.max(20, c.getInt("infection.ticks_per_stage", 1500));
        this.applySlowness = c.getBoolean("infection.apply_slowness", true);
        this.applyHunger = c.getBoolean("infection.apply_hunger", true);
        this.spawnZombiePlayers = c.getBoolean("infection.spawn_zombie_players", true);
        this.aggroConversions = c.getBoolean("infection.aggro_conversions", true);
        this.stageTickReduction = c.getDouble("infection.stage_tick_reduction", 0.95);
        this.showBossBar = c.getBoolean("infection.show_bossbar", true);
        this.showParticles = c.getBoolean("infection.show_particles", true);

        this.zombieGraves = c.getBoolean("zombie_players.zombie_graves", false);
        this.drownedPlayers = c.getBoolean("zombie_players.drowned_players", true);
        this.huskPlayers = c.getBoolean("zombie_players.husk_players", true);
        this.zombiePlayersFireImmune = c.getBoolean("zombie_players.fire_immune", false);
        this.zombiePlayersBurn = c.getBoolean("zombie_players.burn_in_sunlight", false);
        this.zombiePlayersOnlyHurtByPlayers = c.getBoolean("zombie_players.only_hurt_by_players", false);
        this.zombiePlayersStoreItems = c.getBoolean("zombie_players.store_items", true);
        this.zombiePlayersDespawnPeaceful = c.getBoolean("zombie_players.despawn_peaceful", false);
        this.zombiePlayersUsePlayerHead = c.getBoolean("zombie_players.use_player_head", true);

        this.zombiesBurn = c.getBoolean("misc.zombies_burn", true);
        this.skeletonsBurn = c.getBoolean("misc.skeletons_burn", true);
        this.zombieVillagersCanBeCured = c.getBoolean("misc.zombie_villagers_can_be_cured", true);
        this.piglinsHoglinsConvert = c.getBoolean("misc.piglins_hoglins_convert", true);
        this.aggressiveZombieHorses = c.getBoolean("misc.aggressive_zombie_horses", true);
        this.zombieHorsesBurn = c.getBoolean("misc.zombie_horses_burn", false);
        this.skeletonHorsesBurn = c.getBoolean("misc.skeleton_horses_burn", false);
        this.zombiesScareHorses = c.getBoolean("misc.zombies_scare_horses", true);
        this.zombiePiglinsHostile = c.getBoolean("misc.zombie_piglins_hostile", false);
        this.piglinsHuntZombies = c.getBoolean("misc.piglins_hunt_zombies", true);
        this.piglinsCureThemselves = c.getBoolean("misc.piglins_cure_themselves", true);
        this.illagersHuntZombies = c.getBoolean("misc.illagers_hunt_zombies", true);
        this.zoglinsAttackUndead = c.getBoolean("misc.zoglins_attack_undead", false);
        this.zoglinsAttackMobs = c.getBoolean("misc.zoglins_attack_mobs", true);
    }
}
