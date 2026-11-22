package com.geminineon.hordes.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Safely dispatches admin-configured reward commands (PRD FR-03 F-07 & Section 10).
 */
public final class RewardCommandDispatcher {

    private final JavaPlugin plugin;

    public RewardCommandDispatcher(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void dispatchAll(Player player, int day, List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        Location loc = player.getLocation();
        String safeName = player.getName().replaceAll("[^a-zA-Z0-9_]", "");
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";

        for (String raw : commands) {
            if (raw == null || raw.isBlank()) continue;
            String cmd = raw.strip();
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            cmd = cmd
                    .replace("%player%", safeName)
                    .replace("%world%", worldName)
                    .replace("%day%", String.valueOf(day))
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()));

            boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            if (!ok) {
                plugin.getLogger().warning("Reward command failed to execute: " + cmd);
            }
        }
    }
}
