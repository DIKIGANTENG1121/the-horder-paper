package com.geminineon.hordes.core;

import com.geminineon.hordes.compat.PaperCompat;
import com.geminineon.hordes.config.HordesConfig;
import com.geminineon.hordes.config.InfectionConfigData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages 4-stage infection lifecycle, cures, immunity, wearable resistance,
 * non-player conversions, and PDC persistence across server restarts (PRD F-03, F-06, F-10, F-11, AT-09).
 */
public final class InfectionManager {

    private static final int TOTAL_STAGES = 4;

    private final JavaPlugin plugin;
    private final HordesConfig config;
    private final InfectionConfigData infectionData;
    private final ZombiePlayerManager zombiePlayerManager;
    private final Random random = new Random();

    private final NamespacedKey infectedTicksKey;
    private final NamespacedKey infectedStageKey;
    private final NamespacedKey cureCountKey;
    private final NamespacedKey immuneUntilKey;
    private final NamespacedKey infectivityAttrKey;
    private final NamespacedKey resistanceAttrKey;
    private final NamespacedKey variantIdKey;

    private final Map<UUID, BossBar> playerBars = new HashMap<>();
    private BukkitTask tickTask;

    public InfectionManager(JavaPlugin plugin, HordesConfig config, InfectionConfigData infectionData, ZombiePlayerManager zombiePlayerManager) {
        this.plugin = plugin;
        this.config = config;
        this.infectionData = infectionData;
        this.zombiePlayerManager = zombiePlayerManager;

        this.infectedTicksKey = PaperCompat.key(plugin, "infection_ticks");
        this.infectedStageKey = PaperCompat.key(plugin, "infection_stage");
        this.cureCountKey = PaperCompat.key(plugin, "infection_cures");
        this.immuneUntilKey = PaperCompat.key(plugin, "infection_immune_until");
        this.infectivityAttrKey = PaperCompat.key(plugin, "infectivity");
        this.resistanceAttrKey = PaperCompat.key(plugin, "infection_resistance");
        this.variantIdKey = PaperCompat.key(plugin, "variant_id");
    }

    public void start() {
        stop();
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickInfectedEntities, 20L, 20L);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (Map.Entry<UUID, BossBar> entry : playerBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.hideBossBar(entry.getValue());
            }
        }
        playerBars.clear();
    }

    public boolean tryInfect(LivingEntity victim, LivingEntity attacker) {
        if (!config.infectionEnabled || isInfected(victim)) {
            return false;
        }
        if (victim instanceof Player && !config.infectPlayers) {
            return false;
        }
        if (isImmune(victim)) {
            if (victim instanceof Player p) {
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 0.8f, 1.1f);
            }
            return false;
        }

        Double baseChance = infectionData.getInfectionChance(attacker.getType());
        if (baseChance == null) {
            return false;
        }

        double infectivity = attacker.getPersistentDataContainer().getOrDefault(infectivityAttrKey, PersistentDataType.DOUBLE, 1.0);
        double resistance = calculateInfectionResistance(victim);
        double finalChance = Math.max(0.0, Math.min(1.0, baseChance * infectivity * (1.0 - resistance)));

        if (random.nextDouble() < finalChance) {
            infectEntity(victim);
            return true;
        }
        return false;
    }

    public void infectEntity(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        int cures = pdc.getOrDefault(cureCountKey, PersistentDataType.INTEGER, 0);
        int stageTicks = Math.max(40, (int) Math.round(config.ticksPerStage * Math.pow(config.stageTickReduction, cures)));
        int totalTicks = stageTicks * TOTAL_STAGES;

        pdc.set(infectedTicksKey, PersistentDataType.INTEGER, totalTicks);
        pdc.set(infectedStageKey, PersistentDataType.INTEGER, 1);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.7f, 0.6f);
        if (entity instanceof Player player) {
            player.sendActionBar(Component.text("You have been infected!", NamedTextColor.DARK_RED));
            updatePlayerBossBar(player, totalTicks, totalTicks, 1);
        }
    }

    public boolean cureEntity(LivingEntity entity, boolean incrementCureCounter) {
        if (!isInfected(entity)) {
            return false;
        }
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.remove(infectedTicksKey);
        pdc.remove(infectedStageKey);
        if (incrementCureCounter) {
            int prev = pdc.getOrDefault(cureCountKey, PersistentDataType.INTEGER, 0);
            pdc.set(cureCountKey, PersistentDataType.INTEGER, prev + 1);
        }
        if (entity instanceof Player player) {
            removePlayerBossBar(player);
            player.sendActionBar(Component.text("Your infection has been cured.", NamedTextColor.GREEN));
        }
        return true;
    }

    public void grantImmunity(LivingEntity entity, int seconds) {
        long untilMillis = System.currentTimeMillis() + (seconds * 1000L);
        entity.getPersistentDataContainer().set(immuneUntilKey, PersistentDataType.LONG, untilMillis);
    }

    public boolean isInfected(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(infectedTicksKey, PersistentDataType.INTEGER);
    }

    public boolean isImmune(LivingEntity entity) {
        Long until = entity.getPersistentDataContainer().get(immuneUntilKey, PersistentDataType.LONG);
        return until != null && until > System.currentTimeMillis();
    }

    public void resetCureCount(Player player) {
        player.getPersistentDataContainer().remove(cureCountKey);
    }

    public double calculateInfectionResistance(LivingEntity entity) {
        double base = (entity instanceof Player) ? config.playerBaseResistance : 0.0;
        InfectionConfigData.ConversionEntry conv = infectionData.getConversion(entity.getType());
        if (conv != null) {
            base = Math.max(base, conv.protection());
        }
        double customAttr = entity.getPersistentDataContainer().getOrDefault(resistanceAttrKey, PersistentDataType.DOUBLE, 0.0);
        double total = base + customAttr;

        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            for (ItemStack armor : eq.getArmorContents()) {
                total = applyWearable(total, armor);
            }
        }
        if (entity instanceof AbstractHorse horse && horse.getInventory().getItem(1) != null) {
            total = applyWearable(total, horse.getInventory().getItem(1));
        }
        return Math.max(0.0, Math.min(1.0, total));
    }

    private double applyWearable(double current, ItemStack item) {
        if (item == null || item.getType().isAir()) return current;
        InfectionConfigData.WearableProtection wp = infectionData.getWearableProtection(item.getType());
        if (wp == null) return current;
        if ("add_value".equalsIgnoreCase(wp.operation())) {
            return current + wp.protection();
        }
        return current + (1.0 - current) * wp.protection();
    }

    private void tickInfectedEntities() {
        if (!config.infectionEnabled) return;

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (!isInfected(entity) || entity.isDead()) continue;

                PersistentDataContainer pdc = entity.getPersistentDataContainer();
                int remaining = pdc.getOrDefault(infectedTicksKey, PersistentDataType.INTEGER, 0) - 20;
                int cures = pdc.getOrDefault(cureCountKey, PersistentDataType.INTEGER, 0);
                int stageTicks = Math.max(40, (int) Math.round(config.ticksPerStage * Math.pow(config.stageTickReduction, cures)));
                int maxTotal = stageTicks * TOTAL_STAGES;

                if (remaining <= 0) {
                    pdc.remove(infectedTicksKey);
                    pdc.remove(infectedStageKey);
                    handleInfectionExpiration(entity);
                    continue;
                }

                int stage = Math.min(TOTAL_STAGES, TOTAL_STAGES - ((remaining - 1) / stageTicks));
                pdc.set(infectedTicksKey, PersistentDataType.INTEGER, remaining);
                pdc.set(infectedStageKey, PersistentDataType.INTEGER, stage);

                applyStageEffects(entity, stage, remaining, maxTotal);
            }
        }
    }

    private void applyStageEffects(LivingEntity entity, int stage, int remaining, int maxTotal) {
        if (config.applyHunger && stage >= 2 && entity instanceof Player) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, stage - 2, true, false));
        }
        if (config.applySlowness && stage >= 3) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, stage - 3, true, false));
        }
        if (config.showParticles) {
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(160, 175, 40), 1.0f);
            entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1.0, 0), 4, 0.3, 0.5, 0.3, dust);
        }
        if (entity instanceof Player player) {
            updatePlayerBossBar(player, remaining, maxTotal, stage);
        }
    }

    private void handleInfectionExpiration(LivingEntity entity) {
        if (entity instanceof Player player) {
            removePlayerBossBar(player);
            player.getPersistentDataContainer().set(PaperCompat.key(plugin, "died_to_infection"), PersistentDataType.BYTE, (byte) 1);
            player.setHealth(0.0);
            return;
        }
        convertNonPlayerEntity(entity);
    }

    public boolean convertNonPlayerEntity(LivingEntity entity) {
        InfectionConfigData.ConversionEntry conv = infectionData.getConversion(entity.getType());
        if (conv == null) {
            return false;
        }
        Location loc = entity.getLocation();
        Entity converted = loc.getWorld().spawnEntity(loc, conv.convertsTo());
        if (converted instanceof LivingEntity living) {
            if (conv.maxHealth() != null) {
                PaperCompat.setBaseAttribute(living, Attribute.MAX_HEALTH, conv.maxHealth());
                living.setHealth(conv.maxHealth());
            }
            if (conv.customName() != null) {
                living.customName(Component.text(conv.customName()));
            }
            if (conv.variantId() != null) {
                living.getPersistentDataContainer().set(variantIdKey, PersistentDataType.STRING, conv.variantId());
            }
        }
        entity.remove();
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f);
        return true;
    }

    private void updatePlayerBossBar(Player player, int remaining, int maxTotal, int stage) {
        if (!config.showBossBar) return;
        float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / Math.max(1, maxTotal)));
        int seconds = Math.max(0, remaining / 20);
        Component title = Component.text("Infected (Stage " + stage + "/4) — " + seconds + "s", NamedTextColor.RED);
        BossBar bar = playerBars.computeIfAbsent(player.getUniqueId(), id -> {
            BossBar created = BossBar.bossBar(title, progress, BossBar.Color.RED, BossBar.Overlay.NOTCHED_6);
            player.showBossBar(created);
            return created;
        });
        bar.name(title);
        bar.progress(progress);
    }

    public void removePlayerBossBar(Player player) {
        BossBar bar = playerBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }
}
