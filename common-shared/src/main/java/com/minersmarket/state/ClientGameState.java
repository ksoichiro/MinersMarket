package com.minersmarket.state;

import com.minersmarket.hud.GameHudOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientGameState {
    private static GameState state = GameState.NOT_STARTED;
    private static long salesAmount = 0;
    private static int playTime = 0;
    private static List<FinishedEntry> finishedPlayers = Collections.emptyList();

    public record FinishedEntry(String playerName, int finishTimeTicks) {
    }

    public static void update(GameState state, long salesAmount, int playTime,
                              List<FinishedEntry> finishedPlayers) {
        long earned = salesAmount - ClientGameState.salesAmount;
        if (earned > 0) {
            GameHudOverlay.addFloatingText(earned);
        }
        ClientGameState.state = state;
        ClientGameState.salesAmount = salesAmount;
        ClientGameState.playTime = playTime;
        ClientGameState.finishedPlayers = finishedPlayers;
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

    public static void reset() {
        state = GameState.NOT_STARTED;
        salesAmount = 0;
        playTime = 0;
        finishedPlayers = Collections.emptyList();
    }
}

