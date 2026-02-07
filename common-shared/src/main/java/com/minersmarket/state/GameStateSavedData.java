package com.minersmarket.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameStateSavedData extends SavedData {
    GameState state = GameState.NOT_STARTED;
    final Map<UUID, Long> salesAmounts = new HashMap<>();
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
        return tag;
    }
}
