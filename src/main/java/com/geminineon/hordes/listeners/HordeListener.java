package com.geminineon.hordes.listeners;

import com.geminineon.hordes.config.HordesConfig;
import com.geminineon.hordes.core.HordeManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

import java.util.Optional;

public final class HordeListener implements Listener {

    private final HordesConfig config;
    private final HordeManager hordeManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public HordeListener(HordesConfig config, HordeManager hordeManager) {
        this.config = config;
        this.hordeManager = hordeManager;
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (!config.hordeEnabled || config.canSleepDuringHorde) {
            return;
        }
        Player player = event.getPlayer();
        boolean ownActive = hordeManager.getOrCreateSession(player).isActive();
        if (ownActive) {
            event.setCancelled(true);
            player.sendMessage(miniMessage.deserialize(config.msgTrySleep));
            return;
        }
        if (config.preventsOtherPlayersSleeping) {
            Optional<Player> other = hordeManager.getFirstActiveHordePlayer(player.getWorld());
            if (other.isPresent()) {
                event.setCancelled(true);
                String msg = config.msgOtherPlayerTrySleep.replace("%player%", other.get().getName());
                player.sendMessage(miniMessage.deserialize(msg));
            }
        }
    }
}
