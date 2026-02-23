package com.minersmarket.network;

import com.minersmarket.MinersMarket;
import com.minersmarket.state.ClientGameState;
import com.minersmarket.state.FinishedPlayer;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class GameStateSyncPacket {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_state_sync");

    private static BiConsumer<ServerPlayer, GameStateManager> packetSender;

    public static void setPacketSender(BiConsumer<ServerPlayer, GameStateManager> sender) {
        packetSender = sender;
    }

    public static void sendToPlayer(ServerPlayer player, GameStateManager manager) {
        if (packetSender != null) {
            packetSender.accept(player, manager);
        }
    }

    public static void encode(FriendlyByteBuf buf, ServerPlayer player, GameStateManager manager) {
        buf.writeInt(manager.getState().ordinal());
        buf.writeLong(manager.getSalesAmount(player.getUUID()));
        buf.writeInt(manager.getPlayTime());
        List<FinishedPlayer> finished = manager.getFinishedPlayers();
        buf.writeInt(finished.size());
        for (FinishedPlayer fp : finished) {
            buf.writeUtf(fp.playerName());
            buf.writeInt(fp.finishTimeTicks());
        }
        boolean hasEvent = manager.isPriceEventActive();
        buf.writeBoolean(hasEvent);
        if (hasEvent) {
            buf.writeInt(manager.getPriceEventRemainingTicks());
            buf.writeFloat(manager.getPriceMultiplier());
        }
    }

    public static void applyOnClient(FriendlyByteBuf buf) {
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
        boolean hasEvent = buf.readBoolean();
        int eventRemainingTicks = 0;
        float multiplier = 1.0f;
        if (hasEvent) {
            eventRemainingTicks = buf.readInt();
            multiplier = buf.readFloat();
        }
        ClientGameState.update(GameState.values()[stateOrdinal], salesAmount, playTime,
                finishedEntries, hasEvent, eventRemainingTicks, multiplier);
    }
}
