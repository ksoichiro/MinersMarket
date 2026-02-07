package com.minersmarket.state;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;

public class GameStateManager {
    private static final String DATA_ID = "minersmarket_game_state";
    public static final long TARGET_SALES = 10000;

    private static GameStateManager instance;
    private final GameStateSavedData savedData;

    private GameStateManager(GameStateSavedData savedData) {
        this.savedData = savedData;
    }

    public static void init(ServerLevel level) {
        GameStateSavedData data = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GameStateSavedData::new, GameStateSavedData::load, null),
                DATA_ID
        );
        instance = new GameStateManager(data);
    }

    public static GameStateManager getInstance() {
        return instance;
    }

    public static void clear() {
        instance = null;
    }

    // State

    public GameState getState() {
        return savedData.state;
    }

    // State transitions

    public void start() {
        savedData.state = GameState.IN_PROGRESS;
        savedData.playTime = 0;
        savedData.salesAmounts.clear();
        savedData.setDirty();
    }

    public void end() {
        savedData.state = GameState.ENDED;
        savedData.setDirty();
    }

    public void reset() {
        savedData.state = GameState.NOT_STARTED;
        savedData.playTime = 0;
        savedData.salesAmounts.clear();
        savedData.setDirty();
    }

    // Sales

    public long getSalesAmount(UUID playerId) {
        return savedData.salesAmounts.getOrDefault(playerId, 0L);
    }

    public Map<UUID, Long> getAllSalesAmounts() {
        return savedData.salesAmounts;
    }

    public void addSalesAmount(UUID playerId, long amount) {
        long current = getSalesAmount(playerId);
        savedData.salesAmounts.put(playerId, current + amount);
        savedData.setDirty();
    }

    // Play time

    public int getPlayTime() {
        return savedData.playTime;
    }

    public void tick() {
        if (savedData.state == GameState.IN_PROGRESS) {
            savedData.playTime++;
            savedData.setDirty();
        }
    }

    // State checks

    public boolean canSell() {
        return savedData.state == GameState.IN_PROGRESS;
    }

    public boolean canStart() {
        return savedData.state == GameState.NOT_STARTED;
    }

    public boolean canReset() {
        return savedData.state == GameState.IN_PROGRESS || savedData.state == GameState.ENDED;
    }

    // Win check

    public boolean hasReachedTarget(UUID playerId) {
        return getSalesAmount(playerId) >= TARGET_SALES;
    }
}
