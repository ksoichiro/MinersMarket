package com.minersmarket.network;

import com.minersmarket.MinersMarket;
import com.minersmarket.config.GameMode;
import com.minersmarket.state.ClientGameState;
import com.minersmarket.state.FinishedPlayer;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import com.minersmarket.state.RankedPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class GameStateSyncPacket {
    public static final ResourceLocation ID =
            new ResourceLocation(MinersMarket.MOD_ID, "game_state_sync");

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
        buf.writeLong(manager.getTargetSales());
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
        buf.writeInt(manager.getMode().ordinal());
        buf.writeInt(manager.getRemainingTicks());
        // A hidden ranking is not written at all, so it cannot be recovered from the
        // packet by a modified client.
        boolean showRanking = manager.isRankingVisible();
        buf.writeBoolean(showRanking);
        if (showRanking) {
            List<RankedPlayer> ranking = manager.getRanking();
            buf.writeInt(ranking.size());
            for (RankedPlayer entry : ranking) {
                buf.writeInt(entry.rank());
                buf.writeUtf(entry.playerName());
                buf.writeLong(entry.salesAmount());
            }
        }
    }

    public static void applyOnClient(FriendlyByteBuf buf) {
        int stateOrdinal = buf.readInt();
        long salesAmount = buf.readLong();
        long targetSales = buf.readLong();
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
        int modeOrdinal = buf.readInt();
        GameMode mode = GameMode.values()[modeOrdinal];
        int remainingTicks = buf.readInt();
        boolean showRanking = buf.readBoolean();
        List<RankedPlayer> ranking = new ArrayList<>();
        if (showRanking) {
            int rankingCount = buf.readInt();
            for (int i = 0; i < rankingCount; i++) {
                int rank = buf.readInt();
                String name = buf.readUtf();
                long amount = buf.readLong();
                ranking.add(new RankedPlayer(rank, name, amount));
            }
        }
        ClientGameState.update(GameState.values()[stateOrdinal], salesAmount, targetSales, playTime,
                finishedEntries, hasEvent, eventRemainingTicks, multiplier,
                mode, remainingTicks, ranking);
    }
}
