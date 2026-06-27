package com.minersmarket.event;

import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class GameTickHandler {
    private static final int SYNC_INTERVAL = 20;
    private static int syncTimer = 0;

    public static void onServerTick(MinecraftServer server) {
        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null) return;

        manager.tick();

        if (manager.getState() == GameState.IN_PROGRESS || manager.getState() == GameState.ENDED) {
            syncTimer++;
            if (syncTimer >= SYNC_INTERVAL) {
                syncTimer = 0;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    GameStateSyncPacket.sendToPlayer(player, manager);
                }
            }
        }
    }
}
