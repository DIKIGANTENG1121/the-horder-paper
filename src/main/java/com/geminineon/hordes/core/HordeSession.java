package com.geminineon.hordes.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class HordeSession {

    private final UUID playerId;
    private boolean active;
    private int ticksLeft;
    private int ticksUntilNextWave;
    private int nextDay;
    private int hordeCount;
    private String activeTable;
    private final Set<UUID> spawnedEntities = new HashSet<>();

    public HordeSession(UUID playerId, int initialNextDay) {
        this.playerId = playerId;
        this.nextDay = initialNextDay;
    }

    public UUID getPlayerId() { return playerId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getTicksLeft() { return ticksLeft; }
    public void setTicksLeft(int ticksLeft) { this.ticksLeft = ticksLeft; }
    public int getTicksUntilNextWave() { return ticksUntilNextWave; }
    public void setTicksUntilNextWave(int ticks) { this.ticksUntilNextWave = ticks; }
    public int getNextDay() { return nextDay; }
    public void setNextDay(int nextDay) { this.nextDay = nextDay; }
    public int getHordeCount() { return hordeCount; }
    public void setHordeCount(int hordeCount) { this.hordeCount = hordeCount; }
    public String getActiveTable() { return activeTable; }
    public void setActiveTable(String activeTable) { this.activeTable = activeTable; }
    public Set<UUID> getSpawnedEntities() { return spawnedEntities; }
}
