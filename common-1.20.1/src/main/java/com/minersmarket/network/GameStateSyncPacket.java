package com.minersmarket.network;

import com.minersmarket.MinersMarket;
import com.minersmarket.state.ClientGameState;
import com.minersmarket.state.FinishedPlayer;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class GameStateSyncPacket {
    public static final ResourceLocation ID =
            new ResourceLocation(MinersMarket.MOD_ID, "game_state_sync");

    public static void registerClientReceiver() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ID, (buf, context) -> {
            int stateOrdinal = buf.readInt();
            long salesAmount = buf.readLong();
            int playTime = buf.readInt();
            int finishedCount = buf.readInt();
            List<ClientGameState.FinishedEntry> finishedEntries = new ArrayList<>();
            for (int i = 0; i < finishedCount; i++) {
                String name = buf.readUtf();
                int finishTime = buf.readInt();
                finishedEntries.add(new ClientGameState.FinishedEntry(name, finishTime));
            }
            context.queue(() -> {
                ClientGameState.update(GameState.values()[stateOrdinal], salesAmount, playTime, finishedEntries);
            });
        });
    }

    public static void sendToPlayer(ServerPlayer player, GameStateManager manager) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(manager.getState().ordinal());
        buf.writeLong(manager.getSalesAmount(player.getUUID()));
        buf.writeInt(manager.getPlayTime());
        List<FinishedPlayer> finished = manager.getFinishedPlayers();
        buf.writeInt(finished.size());
        for (FinishedPlayer fp : finished) {
            buf.writeUtf(fp.playerName());
            buf.writeInt(fp.finishTimeTicks());
        }
        NetworkManager.sendToPlayer(player, ID, buf);
    }
}
