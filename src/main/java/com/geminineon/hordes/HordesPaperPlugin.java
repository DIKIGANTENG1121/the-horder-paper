package com.geminineon.hordes;

import com.geminineon.hordes.command.HordesCommandRegistrar;
import com.geminineon.hordes.config.HordesConfig;
import com.geminineon.hordes.config.InfectionConfigData;
import com.geminineon.hordes.config.SpawnTableLoader;
import com.geminineon.hordes.core.HordeManager;
import com.geminineon.hordes.core.InfectionManager;
import com.geminineon.hordes.core.ZombiePlayerManager;
import com.geminineon.hordes.listeners.HordeListener;
import com.geminineon.hordes.listeners.InfectionListener;
import com.geminineon.hordes.listeners.MiscBehaviorListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class HordesPaperPlugin extends JavaPlugin {

    private HordesConfig hordesConfig;
    private SpawnTableLoader tableLoader;
    private InfectionConfigData infectionConfigData;
    private HordeManager hordeManager;
    private ZombiePlayerManager zombiePlayerManager;
    private InfectionManager infectionManager;

    @Override
    public void onEnable() {
        this.hordesConfig = new HordesConfig(this);
        this.tableLoader = new SpawnTableLoader(this);
        this.infectionConfigData = new InfectionConfigData(this);

        this.hordeManager = new HordeManager(this, hordesConfig, tableLoader);
        this.zombiePlayerManager = new ZombiePlayerManager(this, hordesConfig);
        this.infectionManager = new InfectionManager(this, hordesConfig, infectionConfigData, zombiePlayerManager);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new HordeListener(hordesConfig, hordeManager), this);
        pm.registerEvents(new InfectionListener(this, hordesConfig, infectionConfigData, infectionManager, zombiePlayerManager), this);
        pm.registerEvents(new MiscBehaviorListener(hordesConfig), this);

        new HordesCommandRegistrar(this).register();

        this.hordeManager.startScheduler();
        this.infectionManager.start();

        getLogger().info("HordesPaper enabled (Paper 26.3 / Java 25, ported from The Hordes 1.21.1-1.6.3f).");
    }

    @Override
    public void onDisable() {
        if (hordeManager != null) {
            hordeManager.shutdown();
        }
        if (infectionManager != null) {
            infectionManager.stop();
        }
        getLogger().info("HordesPaper disabled cleanly.");
    }

    public void reloadAllConfigs() {
        hordesConfig.reload();
        tableLoader.reload();
        infectionConfigData.reload();
    }

    public HordesConfig getHordesConfig() { return hordesConfig; }
    public SpawnTableLoader getTableLoader() { return tableLoader; }
    public InfectionConfigData getInfectionConfigData() { return infectionConfigData; }
    public HordeManager getHordeManager() { return hordeManager; }
    public ZombiePlayerManager getZombiePlayerManager() { return zombiePlayerManager; }
    public InfectionManager getInfectionManager() { return infectionManager; }
}
