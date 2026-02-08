package com.minersmarket.state;

import com.minersmarket.hud.GameHudOverlay;

public class ClientGameState {
    private static GameState state = GameState.NOT_STARTED;
    private static long salesAmount = 0;
    private static int playTime = 0;

    public static void update(GameState state, long salesAmount, int playTime) {
        long earned = salesAmount - ClientGameState.salesAmount;
        if (earned > 0) {
            GameHudOverlay.addFloatingText(earned);
        }
        ClientGameState.state = state;
        ClientGameState.salesAmount = salesAmount;
        ClientGameState.playTime = playTime;
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

    public static void reset() {
        state = GameState.NOT_STARTED;
        salesAmount = 0;
        playTime = 0;
    }
}
