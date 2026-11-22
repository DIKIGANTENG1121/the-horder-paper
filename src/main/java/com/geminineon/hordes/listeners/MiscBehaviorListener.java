package com.geminineon.hordes.listeners;

import com.geminineon.hordes.config.HordesConfig;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Implements The Hordes Misc options via pure Paper 26.3 Event API (PRD FR-03 F-08).
 */
public final class MiscBehaviorListener implements Listener {

    private final HordesConfig config;

    public MiscBehaviorListener(HordesConfig config) {
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (entity.getFireTicks() > 0) return;

        if ((entity instanceof Zombie || entity instanceof Drowned) && !config.zombiesBurn) {
            event.setCancelled(true);
        } else if ((entity instanceof Skeleton || entity instanceof Stray) && !config.skeletonsBurn) {
            event.setCancelled(true);
        } else if (entity instanceof ZombieHorse && !config.zombieHorsesBurn) {
            event.setCancelled(true);
        } else if (entity instanceof SkeletonHorse && !config.skeletonHorsesBurn) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTransform(EntityTransformEvent event) {
        if (!config.piglinsHoglinsConvert
                && event.getTransformReason() == EntityTransformEvent.TransformReason.PIGLIN_ZOMBIFIED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCureZombieVillager(PlayerInteractEntityEvent event) {
        if (!config.zombieVillagersCanBeCured
                && event.getRightClicked() instanceof ZombieVillager zv
                && zv.hasPotionEffect(PotionEffectType.WEAKNESS)
                && event.getPlayer().getInventory().getItem(event.getHand()).getType() == Material.GOLDEN_APPLE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof PigZombie piglin && config.zombiePiglinsHostile) {
            piglin.setAngry(true);
            piglin.setAnger(Integer.MAX_VALUE);
        }
        if (entity instanceof PiglinAbstract piglin && !config.piglinsHoglinsConvert) {
            piglin.setImmuneToZombification(true);
        }
        if (entity instanceof Hoglin hoglin && !config.piglinsHoglinsConvert) {
            hoglin.setImmuneToZombification(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Zoglin && event.getTarget() != null) {
            boolean targetIsUndead = event.getTarget() instanceof Zombie || event.getTarget() instanceof Skeleton;
            if (targetIsUndead && !config.zoglinsAttackUndead) {
                event.setCancelled(true);
            } else if (!targetIsUndead && !(event.getTarget() instanceof Player) && !config.zoglinsAttackMobs) {
                event.setCancelled(true);
            }
        }
    }
}
