package com.geminineon.hordes.core;

import com.geminineon.hordes.compat.PaperCompat;
import com.geminineon.hordes.config.HordesConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Spawns and manages Zombie/Drowned/Husk Players with serialized inventory storage (PRD FR-03 F-04).
 */
public final class ZombiePlayerManager {

    private final JavaPlugin plugin;
    private final HordesConfig config;
    private final NamespacedKey zombiePlayerKey;
    private final NamespacedKey ownerNameKey;
    private final NamespacedKey storedItemsKey;

    public ZombiePlayerManager(JavaPlugin plugin, HordesConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.zombiePlayerKey = PaperCompat.key(plugin, "zombie_player");
        this.ownerNameKey = PaperCompat.key(plugin, "zombie_player_owner");
        this.storedItemsKey = PaperCompat.key(plugin, "zombie_player_items");
    }

    public Zombie spawnZombiePlayer(Player player, Location location, List<ItemStack> drops, EntityType explicitType) {
        EntityType targetType = explicitType != null ? explicitType : resolveVariantType(player, location);
        Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, targetType);

        String displayTitle = switch (targetType) {
            case DROWNED -> "Drowned " + player.getName();
            case HUSK -> player.getName() + "'s Husk";
            default -> player.getName() + "'s Zombie";
        };
        zombie.customName(Component.text(displayTitle));
        zombie.setCustomNameVisible(true);
        zombie.setPersistent(true);
        zombie.setRemoveWhenFarAway(false);
        zombie.setShouldBurnInDay(config.zombiePlayersBurn);

        PersistentDataContainer pdc = zombie.getPersistentDataContainer();
        pdc.set(zombiePlayerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(ownerNameKey, PersistentDataType.STRING, player.getName());

        EntityEquipment eq = zombie.getEquipment();
        if (eq != null) {
            if (config.zombiePlayersUsePlayerHead) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                if (head.getItemMeta() instanceof SkullMeta meta) {
                    meta.setOwningPlayer(player);
                    head.setItemMeta(meta);
                }
                eq.setHelmet(head);
                eq.setHelmetDropChance(0.0f);
            }
            if (player.getInventory().getChestplate() != null) {
                eq.setChestplate(player.getInventory().getChestplate().clone());
                eq.setChestplateDropChance(0.0f);
            }
            if (player.getInventory().getLeggings() != null) {
                eq.setLeggings(player.getInventory().getLeggings().clone());
                eq.setLeggingsDropChance(0.0f);
            }
            if (player.getInventory().getBoots() != null) {
                eq.setBoots(player.getInventory().getBoots().clone());
                eq.setBootsDropChance(0.0f);
            }
            if (!player.getInventory().getItemInMainHand().getType().isAir()) {
                eq.setItemInMainHand(player.getInventory().getItemInMainHand().clone());
                eq.setItemInMainHandDropChance(0.0f);
            }
        }

        if (config.zombiePlayersStoreItems && drops != null && !drops.isEmpty()) {
            ItemStack[] valid = drops.stream()
                    .filter(item -> item != null && !item.getType().isAir())
                    .toArray(ItemStack[]::new);
            if (valid.length > 0) {
                byte[] serialized = ItemStack.serializeItemsAsBytes(valid);
                pdc.set(storedItemsKey, PersistentDataType.BYTE_ARRAY, serialized);
            }
        }
        return zombie;
    }

    public EntityType resolveVariantType(Player player, Location location) {
        if (config.drownedPlayers && (player.isInWater() || location.getBlock().getType() == Material.WATER)) {
            return EntityType.DROWNED;
        }
        if (config.huskPlayers && PaperCompat.isHuskBiome(location.getBlock().getBiome())) {
            return EntityType.HUSK;
        }
        return EntityType.ZOMBIE;
    }

    public boolean isZombiePlayer(Entity entity) {
        return entity.getPersistentDataContainer().has(zombiePlayerKey, PersistentDataType.BYTE);
    }

    public ItemStack[] extractStoredItems(Entity entity) {
        byte[] data = entity.getPersistentDataContainer().get(storedItemsKey, PersistentDataType.BYTE_ARRAY);
        if (data == null || data.length == 0) {
            return new ItemStack[0];
        }
        return ItemStack.deserializeItemsFromBytes(data);
    }
}
