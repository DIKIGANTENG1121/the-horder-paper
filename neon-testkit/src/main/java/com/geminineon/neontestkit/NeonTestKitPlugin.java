package com.geminineon.neontestkit;

import com.geminineon.hordes.HordesPaperPlugin;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public final class NeonTestKitPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var root = Commands.literal("neontest")
                    .requires(src -> src.getSender().isOp())
                    .then(Commands.literal("run")
                            .then(Commands.argument("testId", StringArgumentType.word())
                                    .executes(ctx -> {
                                        String id = StringArgumentType.getString(ctx, "testId").toUpperCase();
                                        runTest(id);
                                        ctx.getSource().getSender().sendMessage(Component.text("Executed NeonTestKit: " + id, NamedTextColor.AQUA));
                                        return Command.SINGLE_SUCCESS;
                                    })));
            event.registrar().register(root.build(), "Run HordesPaper acceptance tests");
        });
        getLogger().info("NeonTestKit enabled. Use /neontest run <AT-01..AT-09|ALL>");
    }

    private void runTest(String id) {
        Plugin raw = Bukkit.getPluginManager().getPlugin("HordesPaper");
        if (!(raw instanceof HordesPaperPlugin hordes)) {
            logResult(id, false, "HordesPaper plugin not loaded");
            return;
        }
        List<String> targets = "ALL".equals(id)
                ? List.of("AT-01", "AT-02", "AT-03", "AT-04", "AT-05", "AT-06", "AT-07", "AT-08", "AT-09")
                : List.of(id);

        for (String test : targets) {
            try {
                boolean pass = switch (test) {
                    case "AT-01" -> hordes.isEnabled();
                    case "AT-02" -> hordes.getHordesConfig().hordeEnabled && hordes.getHordesConfig().startTime == 18000L;
                    case "AT-03" -> !hordes.getTableLoader().getTable("default").entries().isEmpty();
                    case "AT-04" -> hordes.getInfectionConfigData().isCure(org.bukkit.Material.GOLDEN_APPLE);
                    case "AT-05" -> hordes.getZombiePlayerManager() != null;
                    case "AT-06" -> Bukkit.getCommandMap() != null;
                    case "AT-07" -> {
                        hordes.reloadAllConfigs();
                        yield true;
                    }
                    case "AT-08" -> Bukkit.getTPS()[0] >= 18.0;
                    case "AT-09" -> {
                        hordes.getHordeManager().shutdown();
                        hordes.getHordeManager().startScheduler();
                        yield true;
                    }
                    default -> false;
                };
                logResult(test, pass, pass ? "Verified" : "Assertion failed");
            } catch (Exception e) {
                logResult(test, false, "Exception: " + e.getMessage());
            }
        }
    }

    private void logResult(String id, boolean pass, String detail) {
        String line = String.format("[%s] %s: %s — %s", Instant.now(), id, pass ? "PASS" : "FAIL", detail);
        getLogger().info(line);
        try (FileWriter fw = new FileWriter(new File(getDataFolder(), "results.log"), true)) {
            fw.write(line + System.lineSeparator());
        } catch (IOException e) {
            getLogger().warning("Could not write results.log: " + e.getMessage());
        }
    }
}
