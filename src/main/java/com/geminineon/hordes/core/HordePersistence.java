package com.geminineon.hordes.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Persists horde session state across server restarts (PRD AT-09).
 */
public final class HordePersistence {

    private final JavaPlugin plugin;
    private final File stateFile;

    public HordePersistence(JavaPlugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.stateFile = new File(dataDir, "horde-state.yml");
    }

    public Map<UUID, HordeSession> loadAll() {
        Map<UUID, HordeSession> sessions = new HashMap<>();
        if (!stateFile.exists()) {
            return sessions;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        ConfigurationSection sec = yaml.getConfigurationSection("sessions");
        if (sec == null) {
            return sessions;
        }
        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection s = sec.getConfigurationSection(key);
                if (s == null) continue;
                HordeSession session = new HordeSession(uuid, s.getInt("next_day", 10));
                session.setActive(s.getBoolean("active", false));
                session.setTicksLeft(s.getInt("ticks_left", 0));
                session.setTicksUntilNextWave(s.getInt("ticks_until_next_wave", 0));
                session.setHordeCount(s.getInt("horde_count", 0));
                session.setActiveTable(s.getString("active_table", "default"));
                for (String eid : s.getStringList("entities")) {
                    try {
                        session.getSpawnedEntities().add(UUID.fromString(eid));
                    } catch (IllegalArgumentException ignored) {}
                }
                sessions.put(uuid, session);
            } catch (IllegalArgumentException ignored) {}
        }
        return sessions;
    }

    public void saveAll(Map<UUID, HordeSession> sessions) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, HordeSession> entry : sessions.entrySet()) {
            String base = "sessions." + entry.getKey();
            HordeSession s = entry.getValue();
            yaml.set(base + ".active", s.isActive());
            yaml.set(base + ".ticks_left", s.getTicksLeft());
            yaml.set(base + ".ticks_until_next_wave", s.getTicksUntilNextWave());
            yaml.set(base + ".next_day", s.getNextDay());
            yaml.set(base + ".horde_count", s.getHordeCount());
            yaml.set(base + ".active_table", s.getActiveTable());
            List<String> entityIds = s.getSpawnedEntities().stream().map(UUID::toString).toList();
            yaml.set(base + ".entities", entityIds);
        }
        try {
            yaml.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save horde-state.yml: " + e.getMessage());
        }
    }

    public void clearAll() {
        if (stateFile.exists() && !stateFile.delete()) {
            plugin.getLogger().warning("Could not delete horde-state.yml during clean.");
        }
    }
}
