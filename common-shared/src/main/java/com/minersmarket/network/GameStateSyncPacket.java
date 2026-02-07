package com.minersmarket.network;

import com.minersmarket.MinersMarket;
import com.minersmarket.state.ClientGameState;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class GameStateSyncPacket {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_state_sync");

    public static void registerClientReceiver() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ID, (buf, context) -> {
            int stateOrdinal = buf.readInt();
            long salesAmount = buf.readLong();
            int playTime = buf.readInt();
            context.queue(() -> {
                ClientGameState.update(GameState.values()[stateOrdinal], salesAmount, playTime);
            });
        });
    }

    public static void sendToPlayer(ServerPlayer player, GameStateManager manager) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeInt(manager.getState().ordinal());
        buf.writeLong(manager.getSalesAmount(player.getUUID()));
        buf.writeInt(manager.getPlayTime());
        NetworkManager.sendToPlayer(player, ID, buf);
    }
}
