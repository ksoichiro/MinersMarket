package com.minersmarket.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record FinishedPlayer(UUID playerId, String playerName, int finishTimeTicks) {
    public static final Codec<FinishedPlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(FinishedPlayer::playerId),
            Codec.STRING.fieldOf("name").forGetter(FinishedPlayer::playerName),
            Codec.INT.fieldOf("finishTime").forGetter(FinishedPlayer::finishTimeTicks)
    ).apply(instance, FinishedPlayer::new));
}
