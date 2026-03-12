package com.minersmarket.state;

import java.util.Collections;
import java.util.List;
import java.util.function.LongConsumer;

public class ClientGameState {
    private static GameState state = GameState.NOT_STARTED;
    private static long salesAmount = 0;
    private static int playTime = 0;
    private static List<FinishedEntry> finishedPlayers = Collections.emptyList();
    private static boolean priceEventActive = false;
    private static int priceEventRemainingTicks = 0;
    private static float priceMultiplier = 1.0f;
    private static LongConsumer onSaleCallback;

    public static void setOnSaleCallback(LongConsumer callback) {
        onSaleCallback = callback;
    }

    public record FinishedEntry(String playerName, int finishTimeTicks) {
    }

    public static void update(GameState state, long salesAmount, int playTime,
                              List<FinishedEntry> finishedPlayers,
                              boolean priceEventActive, int priceEventRemainingTicks,
                              float priceMultiplier) {
        long earned = salesAmount - ClientGameState.salesAmount;
        if (earned > 0 && onSaleCallback != null) {
            onSaleCallback.accept(earned);
        }
        ClientGameState.state = state;
        ClientGameState.salesAmount = salesAmount;
        ClientGameState.playTime = playTime;
        ClientGameState.finishedPlayers = finishedPlayers;
        ClientGameState.priceEventActive = priceEventActive;
        ClientGameState.priceEventRemainingTicks = priceEventRemainingTicks;
        ClientGameState.priceMultiplier = priceMultiplier;
    }

    public static GameState getState() {
        return state;
    }

    public static long getSalesAmount() {
        return salesAmount;
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

    public static void reset() {
        state = GameState.NOT_STARTED;
        salesAmount = 0;
        playTime = 0;
        finishedPlayers = Collections.emptyList();
        priceEventActive = false;
        priceEventRemainingTicks = 0;
        priceMultiplier = 1.0f;
    }
}
