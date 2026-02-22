package com.minersmarket.state;

import net.minecraft.core.HolderLookup;
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
    boolean marketGenerated = false;

    public GameStateSavedData() {
    }

    public static GameStateSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        GameStateSavedData data = new GameStateSavedData();
        data.state = GameState.values()[tag.getInt("state")];
        data.playTime = tag.getInt("playTime");
        data.marketGenerated = tag.getBoolean("marketGenerated");
        CompoundTag salesTag = tag.getCompound("salesAmounts");
        for (String key : salesTag.getAllKeys()) {
            data.salesAmounts.put(UUID.fromString(key), salesTag.getLong(key));
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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("state", state.ordinal());
        tag.putInt("playTime", playTime);
        tag.putBoolean("marketGenerated", marketGenerated);
        CompoundTag salesTag = new CompoundTag();
        salesAmounts.forEach((uuid, amount) -> salesTag.putLong(uuid.toString(), amount));
        tag.put("salesAmounts", salesTag);
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
