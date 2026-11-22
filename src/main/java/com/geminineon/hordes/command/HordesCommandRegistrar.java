package com.geminineon.hordes.command;

import com.geminineon.hordes.HordesPaperPlugin;
import com.geminineon.hordes.core.HordeSession;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Registers /hordes via Paper 26.3 Brigadier LifecycleEvents.COMMANDS (PRD FR-03 F-05).
 */
public final class HordesCommandRegistrar {

    private final HordesPaperPlugin plugin;

    public HordesCommandRegistrar(HordesPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var root = Commands.literal("hordes")
                    .requires(src -> src.getSender().hasPermission("hordes.command"))

                    // /hordes start [length] [table] [players]
                    .then(Commands.literal("start")
                            .requires(src -> src.getSender().hasPermission("hordes.command.start"))
                            .executes(ctx -> startForTargets(ctx.getSource(), resolveSelf(ctx.getSource()),
                                    plugin.getHordesConfig().spawnDuration, null))
                            .then(Commands.argument("length", IntegerArgumentType.integer(200))
                                    .executes(ctx -> startForTargets(ctx.getSource(), resolveSelf(ctx.getSource()),
                                            IntegerArgumentType.getInteger(ctx, "length"), null))
                                    .then(Commands.argument("table", StringArgumentType.word())
                                            .executes(ctx -> startForTargets(ctx.getSource(), resolveSelf(ctx.getSource()),
                                                    IntegerArgumentType.getInteger(ctx, "length"),
                                                    StringArgumentType.getString(ctx, "table")))
                                            .then(Commands.argument("players", ArgumentTypes.players())
                                                    .executes(ctx -> {
                                                        List<Player> targets = ctx.getArgument("players", PlayerSelectorArgumentResolver.class)
                                                                .resolve(ctx.getSource());
                                                        return startForTargets(ctx.getSource(), targets,
                                                                IntegerArgumentType.getInteger(ctx, "length"),
                                                                StringArgumentType.getString(ctx, "table"));
                                                    })))))

                    // /hordes stop [players]
                    .then(Commands.literal("stop")
                            .requires(src -> src.getSender().hasPermission("hordes.command.stop"))
                            .executes(ctx -> stopForTargets(ctx.getSource(), resolveSelf(ctx.getSource())))
                            .then(Commands.argument("players", ArgumentTypes.players())
                                    .executes(ctx -> stopForTargets(ctx.getSource(),
                                            ctx.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource())))))

                    // /hordes spawnWave <count> [table] [players]
                    .then(Commands.literal("spawnWave")
                            .requires(src -> src.getSender().hasPermission("hordes.command.spawnwave"))
                            .then(Commands.argument("count", IntegerArgumentType.integer(1, 200))
                                    .executes(ctx -> spawnWaveTargets(ctx.getSource(), resolveSelf(ctx.getSource()),
                                            IntegerArgumentType.getInteger(ctx, "count"), null))
                                    .then(Commands.argument("table", StringArgumentType.word())
                                            .executes(ctx -> spawnWaveTargets(ctx.getSource(), resolveSelf(ctx.getSource()),
                                                    IntegerArgumentType.getInteger(ctx, "count"),
                                                    StringArgumentType.getString(ctx, "table")))
                                            .then(Commands.argument("players", ArgumentTypes.players())
                                                    .executes(ctx -> spawnWaveTargets(ctx.getSource(),
                                                            ctx.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()),
                                                            IntegerArgumentType.getInteger(ctx, "count"),
                                                            StringArgumentType.getString(ctx, "table")))))))

                    // /hordes reset [players]
                    .then(Commands.literal("reset")
                            .requires(src -> src.getSender().hasPermission("hordes.command.reset"))
                            .executes(ctx -> resetTargets(ctx.getSource(), resolveSelf(ctx.getSource())))
                            .then(Commands.argument("players", ArgumentTypes.players())
                                    .executes(ctx -> resetTargets(ctx.getSource(),
                                            ctx.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource())))))

                    // /hordes clean
                    .then(Commands.literal("clean")
                            .requires(src -> src.getSender().hasPermission("hordes.command.clean"))
                            .executes(ctx -> {
                                plugin.getHordeManager().cleanAllData();
                                ctx.getSource().getSender().sendMessage(Component.text("Cleared all existing horde world data.", NamedTextColor.GREEN));
                                return Command.SINGLE_SUCCESS;
                            }))

                    // /hordes reload
                    .then(Commands.literal("reload")
                            .requires(src -> src.getSender().hasPermission("hordes.command.reload"))
                            .executes(ctx -> {
                                plugin.reloadAllConfigs();
                                ctx.getSource().getSender().sendMessage(Component.text("Reloaded HordesPaper configuration and tables.", NamedTextColor.GREEN));
                                return Command.SINGLE_SUCCESS;
                            }))

                    // /hordes debug
                    .then(Commands.literal("debug")
                            .requires(src -> src.getSender().hasPermission("hordes.command.debug"))
                            .executes(ctx -> {
                                var sessions = plugin.getHordeManager().getSessionsView();
                                ctx.getSource().getSender().sendMessage(Component.text("HordesPaper active sessions: " + sessions.size(), NamedTextColor.YELLOW));
                                for (HordeSession s : sessions.values()) {
                                    ctx.getSource().getSender().sendMessage(Component.text(
                                            " - " + s.getPlayerId() + " active=" + s.isActive()
                                                    + " ticksLeft=" + s.getTicksLeft()
                                                    + " nextDay=" + s.getNextDay()
                                                    + " mobs=" + s.getSpawnedEntities().size(),
                                            NamedTextColor.GRAY));
                                }
                                return Command.SINGLE_SUCCESS;
                            }))

                    // /hordes listEntities
                    .then(Commands.literal("listEntities")
                            .requires(src -> src.getSender().hasPermission("hordes.command.listentities"))
                            .executes(ctx -> {
                                String tables = String.join(", ", plugin.getTableLoader().getTableNames());
                                ctx.getSource().getSender().sendMessage(Component.text("Loaded spawn tables: " + tables, NamedTextColor.AQUA));
                                return Command.SINGLE_SUCCESS;
                            }))

                    // /hordes spawnZombie <player> [type]
                    .then(Commands.literal("spawnZombie")
                            .requires(src -> src.getSender().hasPermission("hordes.command.spawnzombie"))
                            .then(Commands.argument("username", StringArgumentType.word())
                                    .executes(ctx -> spawnZombieCommand(ctx.getSource(), StringArgumentType.getString(ctx, "username"), "zombie"))
                                    .then(Commands.argument("type", StringArgumentType.word())
                                            .executes(ctx -> spawnZombieCommand(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "username"),
                                                    StringArgumentType.getString(ctx, "type"))))));

            event.registrar().register(root.build(), "Manage The Hordes events and infection");
        });
    }

    private List<Player> resolveSelf(CommandSourceStack source) {
        if (source.getExecutor() instanceof Player p) {
            return List.of(p);
        }
        return List.of();
    }

    private int startForTargets(CommandSourceStack source, List<Player> targets, int length, String table) {
        if (targets.isEmpty()) {
            source.getSender().sendMessage(Component.text("No target players specified.", NamedTextColor.RED));
            return 0;
        }
        for (Player p : targets) {
            plugin.getHordeManager().startHorde(p, length, table);
        }
        source.getSender().sendMessage(Component.text("Started horde event for " + targets.size() + " player(s) (" + length + " ticks).", NamedTextColor.GREEN));
        return targets.size();
    }

    private int stopForTargets(CommandSourceStack source, List<Player> targets) {
        int stopped = 0;
        for (Player p : targets) {
            if (plugin.getHordeManager().stopHorde(p, false)) {
                stopped++;
            }
        }
        source.getSender().sendMessage(Component.text("Stopped active horde event for " + stopped + " player(s).", NamedTextColor.GREEN));
        return stopped;
    }

    private int spawnWaveTargets(CommandSourceStack source, List<Player> targets, int count, String table) {
        int totalSpawned = 0;
        for (Player p : targets) {
            HordeSession s = plugin.getHordeManager().getOrCreateSession(p);
            int day = plugin.getHordeManager().getCurrentDay(p.getWorld());
            totalSpawned += plugin.getHordeManager().spawnWaveForSession(p, s, day, count, table);
        }
        source.getSender().sendMessage(Component.text("Summoned horde wave (" + totalSpawned + " mob(s)).", NamedTextColor.GREEN));
        return totalSpawned;
    }

    private int resetTargets(CommandSourceStack source, List<Player> targets) {
        for (Player p : targets) {
            plugin.getHordeManager().resetPlayer(p);
        }
        source.getSender().sendMessage(Component.text("Reset horde data for " + targets.size() + " player(s).", NamedTextColor.GREEN));
        return targets.size();
    }

    private int spawnZombieCommand(CommandSourceStack source, String username, String typeStr) {
        Player target = Bukkit.getPlayerExact(username);
        if (target == null) {
            source.getSender().sendMessage(Component.text("Player must be online: " + username, NamedTextColor.RED));
            return 0;
        }
        EntityType type = switch (typeStr.toLowerCase(Locale.ROOT)) {
            case "drowned" -> EntityType.DROWNED;
            case "husk" -> EntityType.HUSK;
            default -> EntityType.ZOMBIE;
        };
        plugin.getZombiePlayerManager().spawnZombiePlayer(target, source.getLocation(), List.of(), type);
        source.getSender().sendMessage(Component.text("Spawned " + type.name() + " player variant for " + target.getName(), NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
