package com.geminineon.hordes.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public final class InfectionConfigData {

    public record ConversionEntry(
            EntityType convertsTo,
            double protection,
            Double maxHealth,
            String customName,
            String variantId
    ) {}

    public record WearableProtection(double protection, String operation) {}

    private final JavaPlugin plugin;
    private final Map<EntityType, Double> infectingEntities = new EnumMap<>(EntityType.class);
    private final Set<Material> cures = EnumSet.noneOf(Material.class);
    private final Map<Material, Integer> immunityItems = new EnumMap<>(Material.class);
    private final Map<EntityType, ConversionEntry> conversions = new EnumMap<>(EntityType.class);
    private final Map<Material, WearableProtection> wearables = new EnumMap<>(Material.class);

    public InfectionConfigData(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        infectingEntities.clear();
        cures.clear();
        immunityItems.clear();
        conversions.clear();
        wearables.clear();

        File file = new File(plugin.getDataFolder(), "infection.yml");
        if (!file.exists()) {
            plugin.saveResource("infection.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection infSec = yaml.getConfigurationSection("infecting_entities");
        if (infSec != null) {
            for (String key : infSec.getKeys(false)) {
                EntityType type = parseEntity(key);
                if (type != null) {
                    infectingEntities.put(type, infSec.getDouble(key, 0.75));
                }
            }
        }

        for (String item : yaml.getStringList("cures")) {
            Material mat = parseMaterial(item);
            if (mat != null) cures.add(mat);
        }

        ConfigurationSection immSec = yaml.getConfigurationSection("immunity_items");
        if (immSec != null) {
            for (String key : immSec.getKeys(false)) {
                Material mat = parseMaterial(key);
                if (mat != null) immunityItems.put(mat, immSec.getInt(key, 150));
            }
        }

        ConfigurationSection convSec = yaml.getConfigurationSection("conversions");
        if (convSec != null) {
            for (String key : convSec.getKeys(false)) {
                EntityType source = parseEntity(key);
                ConfigurationSection sub = convSec.getConfigurationSection(key);
                if (source == null || sub == null) continue;
                EntityType target = parseEntity(sub.getString("converts_to"));
                if (target == null) continue;
                Double maxHealth = sub.contains("max_health") ? sub.getDouble("max_health") : null;
                conversions.put(source, new ConversionEntry(
                        target,
                        sub.getDouble("protection", 0.0),
                        maxHealth,
                        sub.getString("custom_name"),
                        sub.getString("variant_id")
                ));
            }
        }

        ConfigurationSection wearSec = yaml.getConfigurationSection("wearable_protection");
        if (wearSec != null) {
            for (String key : wearSec.getKeys(false)) {
                Material mat = parseMaterial(key);
                ConfigurationSection sub = wearSec.getConfigurationSection(key);
                if (mat != null && sub != null) {
                    wearables.put(mat, new WearableProtection(
                            sub.getDouble("protection", 0.0),
                            sub.getString("operation", "add_multiplied_total")
                    ));
                }
            }
        }
    }

    public Double getInfectionChance(EntityType type) {
        return infectingEntities.get(type);
    }

    public boolean isCure(Material material) {
        return cures.contains(material);
    }

    public Integer getImmunityDurationSeconds(Material material) {
        return immunityItems.get(material);
    }

    public ConversionEntry getConversion(EntityType type) {
        return conversions.get(type);
    }

    public WearableProtection getWearableProtection(Material material) {
        return wearables.get(material);
    }

    public Set<EntityType> getConvertibleEntities() {
        return Collections.unmodifiableSet(conversions.keySet());
    }

    private EntityType parseEntity(String name) {
        if (name == null) return null;
        try {
            return EntityType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Material parseMaterial(String name) {
        if (name == null) return null;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
