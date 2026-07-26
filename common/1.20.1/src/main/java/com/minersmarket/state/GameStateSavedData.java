package com.minersmarket.state;

import com.minersmarket.config.ConfigDefaults;
import com.minersmarket.config.GameMode;
import net.minecraft.nbt.CompoundTag;
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

    public GameStateSavedData() {
    }

    public static GameStateSavedData load(CompoundTag tag) {
        GameStateSavedData data = new GameStateSavedData();
        data.state = GameState.values()[tag.getInt("state")];
        data.playTime = tag.getInt("playTime");
        data.targetSales = tag.contains("targetSales") ? tag.getLong("targetSales") : ConfigDefaults.GAME_TARGET_SALES;
        data.mode = readMode(tag);
        data.timeLimitTicks = tag.getInt("timeLimitTicks");
        data.marketGenerated = tag.getBoolean("marketGenerated");
        CompoundTag salesTag = tag.getCompound("salesAmounts");
        for (String key : salesTag.getAllKeys()) {
            data.salesAmounts.put(UUID.fromString(key), salesTag.getLong(key));
        }
        CompoundTag namesTag = tag.getCompound("playerNames");
        for (String key : namesTag.getAllKeys()) {
            data.playerNames.put(UUID.fromString(key), namesTag.getString(key));
        }
        if (tag.contains("finishedPlayers")) {
            CompoundTag finishedTag = tag.getCompound("finishedPlayers");
            int count = finishedTag.getInt("count");
            for (int i = 0; i < count; i++) {
                CompoundTag entry = finishedTag.getCompound("entry_" + i);
                UUID uuid = UUID.fromString(entry.getString("uuid"));
                String name = entry.getString("name");
                int finishTime = entry.getInt("finishTime");
                data.finishedPlayers.add(new FinishedPlayer(uuid, name, finishTime));
            }
        }
        return data;
    }

    // Worlds saved before the time-limit mode existed have no "mode" tag, and an
    // out-of-range ordinal would throw, so both cases fall back to the default.
    private static GameMode readMode(CompoundTag tag) {
        if (!tag.contains("mode")) {
            return ConfigDefaults.GAME_MODE;
        }
        int ordinal = tag.getInt("mode");
        GameMode[] values = GameMode.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ConfigDefaults.GAME_MODE;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("state", state.ordinal());
        tag.putInt("playTime", playTime);
        tag.putLong("targetSales", targetSales);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("timeLimitTicks", timeLimitTicks);
        tag.putBoolean("marketGenerated", marketGenerated);
        CompoundTag salesTag = new CompoundTag();
        salesAmounts.forEach((uuid, amount) -> salesTag.putLong(uuid.toString(), amount));
        tag.put("salesAmounts", salesTag);
        CompoundTag namesTag = new CompoundTag();
        playerNames.forEach((uuid, name) -> namesTag.putString(uuid.toString(), name));
        tag.put("playerNames", namesTag);
        CompoundTag finishedTag = new CompoundTag();
        finishedTag.putInt("count", finishedPlayers.size());
        for (int i = 0; i < finishedPlayers.size(); i++) {
            FinishedPlayer fp = finishedPlayers.get(i);
            CompoundTag entry = new CompoundTag();
            entry.putString("uuid", fp.playerId().toString());
            entry.putString("name", fp.playerName());
            entry.putInt("finishTime", fp.finishTimeTicks());
            finishedTag.put("entry_" + i, entry);
        }
        tag.put("finishedPlayers", finishedTag);
        return tag;
    }
}
