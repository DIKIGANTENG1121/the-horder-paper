package com.geminineon.hordes.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public final class SpawnTableLoader {

    public record SpawnEntry(
            EntityType entityType,
            int weight,
            int firstDay,
            int lastDay,
            Material mainHand,
            Material helmet,
            EntityType passengerType,
            Material passengerHand
    ) {
        public boolean isActiveOnDay(int day) {
            return day >= firstDay && (lastDay <= 0 || day <= lastDay);
        }
    }

    public record SpawnTable(String name, String spawnType, List<SpawnEntry> entries) {
        public Optional<SpawnEntry> pickRandom(int day, Random random) {
            List<SpawnEntry> eligible = entries.stream().filter(e -> e.isActiveOnDay(day)).toList();
            if (eligible.isEmpty()) {
                eligible = entries;
            }
            int totalWeight = eligible.stream().mapToInt(SpawnEntry::weight).sum();
            if (totalWeight <= 0) {
                return Optional.empty();
            }
            int roll = random.nextInt(totalWeight);
            int acc = 0;
            for (SpawnEntry entry : eligible) {
                acc += entry.weight();
                if (roll < acc) {
                    return Optional.of(entry);
                }
            }
            return Optional.of(eligible.getFirst());
        }
    }

    private final JavaPlugin plugin;
    private final Map<String, SpawnTable> tables = new HashMap<>();
    private static final List<String> DEFAULT_TABLES = List.of("default", "drowned", "illagers", "skeletons", "fallback");

    public SpawnTableLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        tables.clear();
        File dir = new File(plugin.getDataFolder(), "tables");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create tables directory.");
        }
        for (String name : DEFAULT_TABLES) {
            File target = new File(dir, name + ".yml");
            if (!target.exists()) {
                plugin.saveResource("tables/" + name + ".yml", false);
            }
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String spawnType = yaml.getString("spawn_type", "ground");
            List<SpawnEntry> entries = new ArrayList<>();
            for (Map<?, ?> raw : yaml.getMapList("entries")) {
                EntityType entity = parseEntity(raw.get("entity"));
                if (entity == null) continue;
                int weight = parseInt(raw.get("weight"), 10);
                int firstDay = parseInt(raw.get("first_day"), 0);
                int lastDay = parseInt(raw.get("last_day"), 0);
                Material mainHand = parseMaterial(raw.get("main_hand"));
                Material helmet = parseMaterial(raw.get("helmet"));
                EntityType passenger = parseEntity(raw.get("passenger"));
                Material passengerHand = parseMaterial(raw.get("passenger_hand"));
                entries.add(new SpawnEntry(entity, weight, firstDay, lastDay, mainHand, helmet, passenger, passengerHand));
            }
            tables.put(name, new SpawnTable(name, spawnType, List.copyOf(entries)));
        }
    }

    public SpawnTable getTable(String name) {
        if (name != null) {
            SpawnTable exact = tables.get(name.toLowerCase(Locale.ROOT));
            if (exact != null && !exact.entries().isEmpty()) {
                return exact;
            }
        }
        return tables.getOrDefault("default", tables.getOrDefault("fallback",
                new SpawnTable("fallback", "ground", List.of(
                        new SpawnEntry(EntityType.ZOMBIE, 30, 0, 0, null, Material.CARVED_PUMPKIN, null, null)
                ))));
    }

    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(tables.keySet());
    }

    private EntityType parseEntity(Object obj) {
        if (obj == null) return null;
        try {
            return EntityType.valueOf(obj.toString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Material parseMaterial(Object obj) {
        if (obj == null) return null;
        try {
            return Material.valueOf(obj.toString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int parseInt(Object obj, int def) {
        if (obj instanceof Number n) return n.intValue();
        try {
            return obj == null ? def : Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
