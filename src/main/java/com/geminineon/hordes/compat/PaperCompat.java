package com.geminineon.hordes.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * Isolated compatibility layer for Paper 26.3 API.
 * Strictly uses public Paper/Bukkit API without NMS.
 */
public final class PaperCompat {

    private PaperCompat() {
    }

    public static NamespacedKey key(Plugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    public static void setBaseAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    public static boolean isOceanBiome(Biome biome) {
        String key = biome.getKey().getKey();
        return key.contains("ocean") && !key.contains("frozen_ocean");
    }

    public static boolean isHuskBiome(Biome biome) {
        String key = biome.getKey().getKey();
        return key.equals("desert") || key.contains("savanna") || key.contains("badlands");
    }
}
