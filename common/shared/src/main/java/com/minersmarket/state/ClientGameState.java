package com.minersmarket.state;

import com.minersmarket.config.ConfigDefaults;
import com.minersmarket.config.GameMode;

import java.util.Collections;
import java.util.List;
import java.util.function.LongConsumer;

public class ClientGameState {
    private static GameState state = GameState.NOT_STARTED;
    private static long salesAmount = 0;
    private static long targetSales = ConfigDefaults.GAME_TARGET_SALES;
    private static int playTime = 0;
    private static List<FinishedEntry> finishedPlayers = Collections.emptyList();
    private static boolean priceEventActive = false;
    private static int priceEventRemainingTicks = 0;
    private static float priceMultiplier = 1.0f;
    private static GameMode mode = ConfigDefaults.GAME_MODE;
    private static int remainingTicks = 0;
    private static List<RankedPlayer> ranking = Collections.emptyList();
    private static LongConsumer onSaleCallback;

    public static void setOnSaleCallback(LongConsumer callback) {
        onSaleCallback = callback;
    }

    public record FinishedEntry(String playerName, int finishTimeTicks) {
    }

    public static void update(GameState state, long salesAmount, long targetSales, int playTime,
                              List<FinishedEntry> finishedPlayers,
                              boolean priceEventActive, int priceEventRemainingTicks,
                              float priceMultiplier,
                              GameMode mode, int remainingTicks, List<RankedPlayer> ranking) {
        long earned = salesAmount - ClientGameState.salesAmount;
        if (earned > 0 && onSaleCallback != null) {
            onSaleCallback.accept(earned);
        }
        ClientGameState.state = state;
        ClientGameState.salesAmount = salesAmount;
        ClientGameState.targetSales = targetSales;
        ClientGameState.playTime = playTime;
        ClientGameState.finishedPlayers = finishedPlayers;
        ClientGameState.priceEventActive = priceEventActive;
        ClientGameState.priceEventRemainingTicks = priceEventRemainingTicks;
        ClientGameState.priceMultiplier = priceMultiplier;
        ClientGameState.mode = mode;
        ClientGameState.remainingTicks = remainingTicks;
        ClientGameState.ranking = ranking;
    }

    public static GameState getState() {
        return state;
    }

    public static long getSalesAmount() {
        return salesAmount;
    }

    public static long getTargetSales() {
        return targetSales;
    }

    public static int getPlayTime() {
        return playTime;
    }

    public static List<FinishedEntry> getFinishedPlayers() {
        return finishedPlayers;
    }

    public static boolean isPriceEventActive() {
        return priceEventActive;
    }

    public static int getPriceEventRemainingTicks() {
        return priceEventRemainingTicks;
    }

    public static float getPriceMultiplier() {
        return priceMultiplier;
    }

    public static GameMode getMode() {
        return mode;
    }

    public static int getRemainingTicks() {
        return remainingTicks;
    }

    /** Empty while the ranking is hidden — the server does not send it. */
    public static List<RankedPlayer> getRanking() {
        return ranking;
    }

    public static void reset() {
        state = GameState.NOT_STARTED;
        salesAmount = 0;
        targetSales = ConfigDefaults.GAME_TARGET_SALES;
        playTime = 0;
        finishedPlayers = Collections.emptyList();
        priceEventActive = false;
        priceEventRemainingTicks = 0;
        priceMultiplier = 1.0f;
        mode = ConfigDefaults.GAME_MODE;
        remainingTicks = 0;
        ranking = Collections.emptyList();
    }
}
