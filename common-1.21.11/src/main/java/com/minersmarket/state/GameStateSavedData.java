package com.minersmarket.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameStateSavedData extends SavedData {
    GameState state = GameState.NOT_STARTED;
    final Map<UUID, Long> salesAmounts = new HashMap<>();
    final List<FinishedPlayer> finishedPlayers = new ArrayList<>();
    int playTime = 0;
    boolean marketGenerated = false;

    public static final Codec<GameStateSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("state", 0).forGetter(d -> d.state.ordinal()),
            Codec.INT.optionalFieldOf("playTime", 0).forGetter(d -> d.playTime),
            Codec.BOOL.optionalFieldOf("marketGenerated", false).forGetter(d -> d.marketGenerated),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("salesAmounts", Map.of())
                    .forGetter(d -> {
                        Map<String, Long> map = new HashMap<>();
                        d.salesAmounts.forEach((uuid, amount) -> map.put(uuid.toString(), amount));
                        return map;
                    }),
            FinishedPlayer.CODEC.listOf().optionalFieldOf("finishedPlayers", List.of())
                    .forGetter(d -> d.finishedPlayers)
    ).apply(instance, GameStateSavedData::fromCodec));

    public GameStateSavedData() {
    }

    private static GameStateSavedData fromCodec(int stateOrdinal, int playTime, boolean marketGenerated,
                                                Map<String, Long> salesMap, List<FinishedPlayer> finishedPlayers) {
        GameStateSavedData data = new GameStateSavedData();
        data.state = GameState.values()[stateOrdinal];
        data.playTime = playTime;
        data.marketGenerated = marketGenerated;
        salesMap.forEach((key, value) -> data.salesAmounts.put(UUID.fromString(key), value));
        data.finishedPlayers.addAll(finishedPlayers);
        return data;
    }
}
