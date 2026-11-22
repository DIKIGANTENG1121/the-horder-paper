package com.geminineon.hordes.core;

import com.geminineon.hordes.compat.PaperCompat;
import com.geminineon.hordes.config.HordesConfig;
import com.geminineon.hordes.config.SpawnTableLoader;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Handles spawn position selection, entity instantiation, equipment/passengers,
 * and PDC tagging so passengers do not consume mob cap (PRD FR-03 F-02).
 */
public final class HordeSpawnEngine {

    private final JavaPlugin plugin;
    private final HordesConfig config;
    private final NamespacedKey hordeMobKey;
    private final NamespacedKey hordeOwnerKey;
    private final NamespacedKey hordePassengerKey;
    private final Random random = new Random();

    public HordeSpawnEngine(JavaPlugin plugin, HordesConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.hordeMobKey = PaperCompat.key(plugin, "horde_mob");
        this.hordeOwnerKey = PaperCompat.key(plugin, "horde_owner");
        this.hordePassengerKey = PaperCompat.key(plugin, "is_passenger");
    }

    public List<Entity> spawnWave(Player player, SpawnTableLoader.SpawnTable table, int count, int day) {
        List<Entity> spawnedPrimary = new ArrayList<>();
        World world = player.getWorld();

        for (int i = 0; i < count; i++) {
            Optional<SpawnTableLoader.SpawnEntry> picked = table.pickRandom(day, random);
            if (picked.isEmpty()) {
                break;
            }
            SpawnTableLoader.SpawnEntry entry = picked.get();
            Location loc = findSpawnLocation(player, "prefer_water".equalsIgnoreCase(table.spawnType()));
            if (loc == null) {
                continue;
            }
            Entity entity = world.spawnEntity(loc, entry.entityType());
            configureHordeEntity(entity, player, false);
            applyEquipment(entity, entry.mainHand(), entry.helmet());

            if (entry.passengerType() != null) {
                Entity passenger = world.spawnEntity(loc, entry.passengerType());
                configureHordeEntity(passenger, player, true);
                applyEquipment(passenger, entry.passengerHand(), null);
                entity.addPassenger(passenger);
            }
            spawnedPrimary.add(entity);
        }
        return spawnedPrimary;
    }

    private void configureHordeEntity(Entity entity, Player owner, boolean isPassenger) {
        entity.getPersistentDataContainer().set(hordeMobKey, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(hordeOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        if (isPassenger) {
            entity.getPersistentDataContainer().set(hordePassengerKey, PersistentDataType.BYTE, (byte) 1);
        }
        if (entity instanceof LivingEntity living) {
            PaperCompat.setBaseAttribute(living, Attribute.FOLLOW_RANGE, config.spawnDistance);
            if (config.entitySpeed != 1.0 && living.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                double base = living.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue();
                living.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(base * config.entitySpeed);
            }
        }
        if (entity instanceof Mob mob) {
            mob.setTarget(owner);
        }
    }

    private void applyEquipment(Entity entity, Material mainHand, Material helmet) {
        if (!(entity instanceof LivingEntity living)) return;
        EntityEquipment eq = living.getEquipment();
        if (eq == null) return;
        if (mainHand != null) {
            eq.setItemInMainHand(new ItemStack(mainHand));
        }
        if (helmet != null) {
            eq.setHelmet(new ItemStack(helmet));
        }
    }

    public Location findSpawnLocation(Player player, boolean preferWater) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        double minDist = Math.min(config.minSpawnDistance, config.spawnDistance - 4.0);
        double maxDist = config.spawnDistance;

        Location fallback = null;
        for (int attempt = 0; attempt < config.spawnChecks; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = minDist + random.nextDouble() * Math.max(1.0, maxDist - minDist);
            int x = (int) Math.floor(origin.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(origin.getZ() + Math.sin(angle) * dist);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block above2 = world.getBlockAt(x, y + 2, z);

            boolean validSurface = preferWater
                    ? (ground.getType() == Material.WATER || ground.getType().isSolid())
                    : (ground.getType().isSolid() && !ground.isLiquid());

            if (validSurface && above.isPassable() && above2.isPassable()) {
                Location candidate = new Location(world, x + 0.5, y + 1.0, z + 0.5);
                if (fallback == null) {
                    fallback = candidate;
                }
                if (above.getLightFromBlocks() <= 7) {
                    return candidate;
                }
            }
        }
        return fallback;
    }

    public boolean isHordeMob(Entity entity) {
        return entity.getPersistentDataContainer().has(hordeMobKey, PersistentDataType.BYTE);
    }

    public boolean isPassengerMob(Entity entity) {
        return entity.getPersistentDataContainer().has(hordePassengerKey, PersistentDataType.BYTE);
    }
}
