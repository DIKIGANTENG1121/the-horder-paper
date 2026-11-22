package com.geminineon.hordes.core;

import com.geminineon.hordes.compat.PaperCompat;
import com.geminineon.hordes.config.HordesConfig;
import com.geminineon.hordes.config.SpawnTableLoader;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Core scheduler & lifecycle manager for Horde Events (PRD FR-03 F-01).
 * All Bukkit API access runs strictly on the main server thread.
 */
public final class HordeManager {

    private final JavaPlugin plugin;
    private final HordesConfig config;
    private final SpawnTableLoader tableLoader;
    private final HordeSpawnEngine spawnEngine;
    private final RewardCommandDispatcher rewardDispatcher;
    private final HordePersistence persistence;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Random random = new Random();

    private final Map<UUID, HordeSession> sessions = new HashMap<>();
    private BukkitTask mainTask;
    private int tickCounter = 0;

    public HordeManager(JavaPlugin plugin, HordesConfig config, SpawnTableLoader tableLoader) {
        this.plugin = plugin;
        this.config = config;
        this.tableLoader = tableLoader;
        this.spawnEngine = new HordeSpawnEngine(plugin, config);
        this.rewardDispatcher = new RewardCommandDispatcher(plugin);
        this.persistence = new HordePersistence(plugin);
        this.sessions.putAll(persistence.loadAll());
    }

    public void startScheduler() {
        stopScheduler();
        this.mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stopScheduler() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
    }

    public void shutdown() {
        stopScheduler();
        persistence.saveAll(sessions);
    }

    private void tick() {
        if (!config.hordeEnabled) return;
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (config.pauseWhenEmpty && online.isEmpty()) return;

        tickCounter += 20;
        boolean dirty = false;

        for (Player player : online) {
            World world = player.getWorld();
            if (!config.enabledWorlds.isEmpty() && !config.enabledWorlds.contains(world.getName())) {
                continue;
            }
            HordeSession session = getOrCreateSession(player);
            int currentDay = getCurrentDay(world);
            long dayTime = world.getTime() % config.dayLength;

            if (!session.isActive()) {
                if (!config.commandOnly && currentDay >= session.getNextDay()
                        && dayTime >= config.startTime && dayTime <= config.startTime + config.startBuffer) {
                    startHorde(player, config.spawnDuration, resolveDefaultTable(player));
                    dirty = true;
                }
            } else {
                pruneDeadEntities(session);
                session.setTicksLeft(session.getTicksLeft() - 20);
                session.setTicksUntilNextWave(session.getTicksUntilNextWave() - 20);

                if (session.getTicksLeft() <= 0) {
                    stopHorde(player, true);
                    dirty = true;
                    continue;
                }

                if (session.getTicksUntilNextWave() <= 0) {
                    spawnWaveForSession(player, session, currentDay, null, null);
                    session.setTicksUntilNextWave(config.spawnInterval);
                    dirty = true;
                }

                if (tickCounter % Math.max(20, config.pathingInterval) == 0) {
                    retargetEntities(player, session);
                }
            }
        }

        if (dirty) {
            persistence.saveAll(sessions);
        }
    }

    public boolean startHorde(Player player, int durationTicks, String tableName) {
        HordeSession session = getOrCreateSession(player);
        session.setActive(true);
        session.setTicksLeft(durationTicks);
        session.setTicksUntilNextWave(0);
        session.setActiveTable(tableName != null ? tableName : resolveDefaultTable(player));
        session.setHordeCount(session.getHordeCount() + 1);

        int currentDay = getCurrentDay(player.getWorld());
        int variation = config.spawnVariation > 0 ? random.nextInt(config.spawnVariation + 1) : 0;
        session.setNextDay(currentDay + config.spawnDays + variation);

        player.sendMessage(miniMessage.deserialize(config.msgEventStart));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.5f, 0.5f);

        spawnWaveForSession(player, session, currentDay, null, session.getActiveTable());
        session.setTicksUntilNextWave(config.spawnInterval);
        persistence.saveAll(sessions);
        return true;
    }

    public boolean stopHorde(Player player, boolean completedNaturally) {
        HordeSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.isActive()) {
            return false;
        }
        session.setActive(false);
        session.setTicksLeft(0);
        player.sendMessage(miniMessage.deserialize(config.msgEventEnd));

        if (completedNaturally) {
            rewardDispatcher.dispatchAll(player, getCurrentDay(player.getWorld()), config.rewardCommands);
        }
        persistence.saveAll(sessions);
        return true;
    }

    public int spawnWaveForSession(Player player, HordeSession session, int day, Integer explicitCount, String tableOverride) {
        pruneDeadEntities(session);
        int currentCap = session.getSpawnedEntities().size();
        int availableSlots = config.spawnMax - currentCap;
        if (availableSlots <= 0) {
            return 0;
        }

        int desired;
        if (explicitCount != null) {
            desired = explicitCount;
        } else {
            double scaled = config.spawnAmount * Math.pow(config.spawnMultiplier, Math.max(0, session.getHordeCount() - 1));
            long nearbyPlayers = player.getWorld().getPlayers().stream()
                    .filter(p -> !p.equals(player) && p.getLocation().distanceSquared(player.getLocation()) <= 6400.0)
                    .count();
            if (nearbyPlayers > 0) {
                scaled *= Math.pow(config.multiplayerScaling, nearbyPlayers);
            }
            desired = Math.max(1, (int) Math.round(scaled));
        }

        int toSpawn = Math.min(desired, availableSlots);
        String chosenTable = tableOverride != null ? tableOverride : session.getActiveTable();
        SpawnTableLoader.SpawnTable table = tableLoader.getTable(chosenTable);
        List<Entity> spawned = spawnEngine.spawnWave(player, table, toSpawn, day);
        for (Entity e : spawned) {
            session.getSpawnedEntities().add(e.getUniqueId());
        }
        return spawned.size();
    }

    public void resetPlayer(Player player) {
        int currentDay = getCurrentDay(player.getWorld());
        int next = config.spawnFirstDay ? currentDay : currentDay + config.spawnDays;
        HordeSession session = new HordeSession(player.getUniqueId(), next);
        sessions.put(player.getUniqueId(), session);
        persistence.saveAll(sessions);
    }

    public void cleanAllData() {
        sessions.clear();
        persistence.clearAll();
    }

    public boolean isAnyHordeActive(World world) {
        return sessions.values().stream().anyMatch(s -> {
            if (!s.isActive()) return false;
            Player p = Bukkit.getPlayer(s.getPlayerId());
            return p != null && p.getWorld().equals(world);
        });
    }

    public Optional<Player> getFirstActiveHordePlayer(World world) {
        for (HordeSession s : sessions.values()) {
            if (s.isActive()) {
                Player p = Bukkit.getPlayer(s.getPlayerId());
                if (p != null && p.getWorld().equals(world)) {
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    public HordeSession getOrCreateSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), uuid -> {
            int currentDay = getCurrentDay(player.getWorld());
            int initialNextDay = config.spawnFirstDay ? currentDay : currentDay + config.spawnDays;
            return new HordeSession(uuid, initialNextDay);
        });
    }

    public int getCurrentDay(World world) {
        return (int) (world.getFullTime() / config.dayLength);
    }

    public HordeSpawnEngine getSpawnEngine() {
        return spawnEngine;
    }

    public Map<UUID, HordeSession> getSessionsView() {
        return Collections.unmodifiableMap(sessions);
    }

    private String resolveDefaultTable(Player player) {
        var biome = player.getLocation().getBlock().getBiome();
        if (PaperCompat.isOceanBiome(biome)) {
            return config.oceanTable;
        }
        return config.defaultTable;
    }

    private void pruneDeadEntities(HordeSession session) {
        session.getSpawnedEntities().removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || !entity.isValid() || entity.isDead();
        });
    }

    private void retargetEntities(Player player, HordeSession session) {
        for (UUID eid : session.getSpawnedEntities()) {
            Entity entity = Bukkit.getEntity(eid);
            if (entity instanceof Mob mob && mob.isValid() && mob.getTarget() == null) {
                mob.setTarget(player);
            }
        }
    }
}
