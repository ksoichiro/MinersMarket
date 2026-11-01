package com.minersmarket.state;

import com.minersmarket.config.ConfigDefaults;
import com.minersmarket.config.GameMode;
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
    long targetSales = ConfigDefaults.GAME_TARGET_SALES;
    boolean marketGenerated = false;
    GameMode mode = ConfigDefaults.GAME_MODE;
    int timeLimitTicks = 0;
    final Map<UUID, String> playerNames = new HashMap<>();

    public static final Codec<GameStateSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("state", 0).forGetter(d -> d.state.ordinal()),
            Codec.INT.optionalFieldOf("playTime", 0).forGetter(d -> d.playTime),
            Codec.LONG.optionalFieldOf("targetSales", ConfigDefaults.GAME_TARGET_SALES).forGetter(d -> d.targetSales),
            Codec.BOOL.optionalFieldOf("marketGenerated", false).forGetter(d -> d.marketGenerated),
            Codec.INT.optionalFieldOf("mode", ConfigDefaults.GAME_MODE.ordinal()).forGetter(d -> d.mode.ordinal()),
            Codec.INT.optionalFieldOf("timeLimitTicks", 0).forGetter(d -> d.timeLimitTicks),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("playerNames", Map.of())
                    .forGetter(d -> {
                        Map<String, String> map = new HashMap<>();
                        d.playerNames.forEach((uuid, name) -> map.put(uuid.toString(), name));
                        return map;
                    }),
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

    private static GameStateSavedData fromCodec(int stateOrdinal, int playTime, long targetSales, boolean marketGenerated,
                                                int modeOrdinal, int timeLimitTicks, Map<String, String> nameMap,
                                                Map<String, Long> salesMap, List<FinishedPlayer> finishedPlayers) {
        GameStateSavedData data = new GameStateSavedData();
        data.state = GameState.values()[stateOrdinal];
        data.playTime = playTime;
        data.targetSales = targetSales;
        data.marketGenerated = marketGenerated;
        // An out-of-range ordinal from a newer build would throw, so fall back instead.
        GameMode[] modes = GameMode.values();
        data.mode = modeOrdinal >= 0 && modeOrdinal < modes.length ? modes[modeOrdinal] : ConfigDefaults.GAME_MODE;
        data.timeLimitTicks = timeLimitTicks;
        nameMap.forEach((key, value) -> data.playerNames.put(UUID.fromString(key), value));
        salesMap.forEach((key, value) -> data.salesAmounts.put(UUID.fromString(key), value));
        data.finishedPlayers.addAll(finishedPlayers);
        return data;
    }
}
