package com.geminineon.hordes.listeners;

import com.geminineon.hordes.compat.PaperCompat;
import com.geminineon.hordes.config.HordesConfig;
import com.geminineon.hordes.config.InfectionConfigData;
import com.geminineon.hordes.core.InfectionManager;
import com.geminineon.hordes.core.ZombiePlayerManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class InfectionListener implements Listener {

    private final JavaPlugin plugin;
    private final HordesConfig config;
    private final InfectionConfigData infectionData;
    private final InfectionManager infectionManager;
    private final ZombiePlayerManager zombiePlayerManager;
    private final NamespacedKey diedToInfectionKey;

    public InfectionListener(JavaPlugin plugin, HordesConfig config, InfectionConfigData infectionData,
                             InfectionManager infectionManager, ZombiePlayerManager zombiePlayerManager) {
        this.plugin = plugin;
        this.config = config;
        this.infectionData = infectionData;
        this.infectionManager = infectionManager;
        this.zombiePlayerManager = zombiePlayerManager;
        this.diedToInfectionKey = PaperCompat.key(plugin, "died_to_infection");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (zombiePlayerManager.isZombiePlayer(victim) && config.zombiePlayersOnlyHurtByPlayers
                && !(event.getDamager() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof LivingEntity attacker) {
            infectionManager.tryInfect(victim, attacker);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onZombiePlayerDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!zombiePlayerManager.isZombiePlayer(entity)) return;
        if (config.zombiePlayersFireImmune && (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        var type = event.getItem().getType();
        if (infectionData.isCure(type)) {
            infectionManager.cureEntity(player, true);
        }
        Integer immunitySec = infectionData.getImmunityDurationSeconds(type);
        if (immunitySec != null && immunitySec > 0) {
            infectionManager.grantImmunity(player, immunitySec);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean wasInfected = infectionManager.isInfected(player)
                || player.getPersistentDataContainer().has(diedToInfectionKey, PersistentDataType.BYTE);

        player.getPersistentDataContainer().remove(diedToInfectionKey);
        infectionManager.cureEntity(player, false);
        infectionManager.resetCureCount(player);

        if ((wasInfected && config.spawnZombiePlayers) || config.zombieGraves) {
            List<ItemStack> dropsCopy = new ArrayList<>(event.getDrops());
            if (config.zombiePlayersStoreItems) {
                event.getDrops().clear();
            }
            zombiePlayerManager.spawnZombiePlayer(player, player.getLocation(), dropsCopy, null);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;

        if (zombiePlayerManager.isZombiePlayer(entity)) {
            ItemStack[] stored = zombiePlayerManager.extractStoredItems(entity);
            if (stored.length > 0) {
                event.getDrops().addAll(Arrays.asList(stored));
            }
            return;
        }

        if (infectionManager.isInfected(entity)) {
            infectionManager.convertNonPlayerEntity(entity);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        infectionManager.removePlayerBossBar(event.getPlayer());
    }
}
